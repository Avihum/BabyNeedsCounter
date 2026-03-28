package com.example.babyneedscounter

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SyncConfig {
    const val PERIODIC_REFRESH_MINUTES = 15L
    const val FOREGROUND_REFRESH_DEBOUNCE_MS = 60_000L
    const val WIDGET_REFRESH_DEBOUNCE_MS = 60_000L
}

internal object RefreshDebouncePolicy {
    fun shouldDebounce(
        trigger: RefreshTrigger,
        lastAttemptAt: Long?,
        nowMs: Long,
    ): Boolean {
        val debounceMs = when (trigger) {
            RefreshTrigger.Resume -> SyncConfig.FOREGROUND_REFRESH_DEBOUNCE_MS
            RefreshTrigger.Widget -> SyncConfig.WIDGET_REFRESH_DEBOUNCE_MS
            else -> return false
        }
        val lastAttempt = lastAttemptAt ?: return false
        return nowMs - lastAttempt < debounceMs
    }
}

enum class RefreshTrigger {
    Initial,
    Resume,
    Manual,
    Mutation,
    Worker,
    Widget,
}

data class RefreshResult(
    val success: Boolean,
    val appliedNetworkUpdate: Boolean,
    val errorMessage: String? = null,
)

data class BabyUiState(
    val currentSourceUrl: String = "",
    val homeSnapshot: CachedHomeSnapshot? = null,
    val eventsSnapshot: CachedEventsSnapshot? = null,
    val isInitializing: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
) {
    val homeStats: BackendService.TodayStats?
        get() = homeSnapshot?.stats

    val recentEvents: List<BackendService.EventItem>
        get() = eventsSnapshot?.events.orEmpty()

    val hasCachedContent: Boolean
        get() = homeSnapshot != null || recentEvents.isNotEmpty()
}

data class WidgetHomeSnapshot(
    val sourceUrl: String,
    val stats: BackendService.TodayStats?,
    val hasCachedContent: Boolean,
    val lastAttemptAt: Long?,
    val lastSuccessAt: Long?,
)

class BabyRepository(context: Context) {
    private val appContext = context.applicationContext
    private val settingsManager = SettingsManager(appContext)
    private val backendService = BackendService(appContext)
    private val statsCache = StatsCache(appContext)
    private val refreshMutex = Mutex()

    private val _uiState = MutableStateFlow(BabyUiState())
    val uiState = _uiState.asStateFlow()

    private var currentSourceUrl: String = ""

    suspend fun setSourceUrl(sourceUrl: String) {
        BabySyncScheduler.ensureScheduled(appContext)
        currentSourceUrl = sourceUrl

        if (sourceUrl.isBlank()) {
            _uiState.value = BabyUiState(currentSourceUrl = sourceUrl, isInitializing = false)
            return
        }

        loadCachedState(sourceUrl)
    }

    suspend fun refresh(trigger: RefreshTrigger, force: Boolean = false): RefreshResult {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) {
            _uiState.value = BabyUiState(currentSourceUrl = sourceUrl, isInitializing = false)
            return RefreshResult(
                success = false,
                appliedNetworkUpdate = false,
                errorMessage = "Please set up your sheet in Settings first",
            )
        }

        val now = System.currentTimeMillis()
        if (!force && RefreshDebouncePolicy.shouldDebounce(trigger, _uiState.value.lastAttemptAt, now)) {
            return RefreshResult(success = true, appliedNetworkUpdate = false)
        }

        return refreshMutex.withLock {
            val before = _uiState.value
            _uiState.value = before.copy(
                currentSourceUrl = sourceUrl,
                isInitializing = before.isInitializing && !before.hasCachedContent,
                isRefreshing = true,
                lastAttemptAt = now,
            )
            statsCache.saveState(before.homeSnapshot, before.eventsSnapshot, now, before.lastSuccessAt)

            val (networkStats, networkEvents) = coroutineScope {
                val statsDeferred = async { backendService.fetchTodayStats(sourceUrl) }
                val eventsDeferred = async { backendService.fetchInsightsEvents(sourceUrl) }
                statsDeferred.await() to eventsDeferred.await()
            }

            if (networkStats == null && networkEvents == null) {
                val failedState = _uiState.value.copy(isInitializing = false, isRefreshing = false, lastAttemptAt = now)
                _uiState.value = failedState
                statsCache.saveState(failedState.homeSnapshot, failedState.eventsSnapshot, failedState.lastAttemptAt, failedState.lastSuccessAt)
                return@withLock RefreshResult(
                    success = false,
                    appliedNetworkUpdate = false,
                    errorMessage = "Couldn't refresh. Check your connection.",
                )
            }

            val lastSuccessAt = now
            val eventsSnapshot = when {
                networkEvents != null -> {
                    val window = StatsAggregation.insightsFetchWindow(now, ZoneId.systemDefault())
                    CachedEventsSnapshot(
                        sourceUrl = sourceUrl,
                        fetchStartMs = window.startMs,
                        fetchEndMs = window.endMs,
                        events = networkEvents,
                        lastSyncedAt = lastSuccessAt,
                    )
                }
                else -> before.eventsSnapshot
            }

            val resolvedStats = resolveHomeStats(
                networkStats = networkStats,
                cachedEvents = eventsSnapshot?.events.orEmpty(),
                nowMs = now,
            ) ?: before.homeSnapshot?.stats

            val homeSnapshot = resolvedStats?.let {
                CachedHomeSnapshot(
                    sourceUrl = sourceUrl,
                    babyDayStartMs = BabyDay.babyDayStartContaining(now, ZoneId.systemDefault()),
                    stats = it,
                    lastSyncedAt = lastSuccessAt,
                )
            } ?: before.homeSnapshot

            val refreshedState = before.copy(
                currentSourceUrl = sourceUrl,
                homeSnapshot = homeSnapshot,
                eventsSnapshot = eventsSnapshot,
                isInitializing = false,
                isRefreshing = false,
                lastAttemptAt = now,
                lastSuccessAt = lastSuccessAt,
            )
            _uiState.value = refreshedState
            statsCache.saveState(homeSnapshot, eventsSnapshot, now, lastSuccessAt)
            WidgetUpdater.requestUpdateAll(appContext)

            RefreshResult(success = true, appliedNetworkUpdate = true)
        }
    }

    suspend fun cachedHomeStats(): BackendService.TodayStats? {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) return null
        val cached = statsCache.loadState(sourceUrl)
        return resolveHomeSnapshot(cached.homeSnapshot, cached.eventsSnapshot)?.stats
    }

    suspend fun widgetSnapshot(): WidgetHomeSnapshot {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) {
            return WidgetHomeSnapshot(
                sourceUrl = "",
                stats = null,
                hasCachedContent = false,
                lastAttemptAt = null,
                lastSuccessAt = null,
            )
        }

        val cached = statsCache.loadState(sourceUrl)
        val resolvedHome = resolveHomeSnapshot(
            homeSnapshot = cached.homeSnapshot,
            eventsSnapshot = cached.eventsSnapshot,
        )

        return WidgetHomeSnapshot(
            sourceUrl = sourceUrl,
            stats = resolvedHome?.stats,
            hasCachedContent = resolvedHome != null || cached.eventsSnapshot?.events?.isNotEmpty() == true,
            lastAttemptAt = cached.lastAttemptAt,
            lastSuccessAt = cached.lastSuccessAt,
        )
    }

    suspend fun logEvent(event: BackendService.BabyEvent): BackendService.LogEventResult {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) return BackendService.LogEventResult(success = false)
        val result = backendService.logEvent(sourceUrl, event)
        if (result.success) {
            refresh(RefreshTrigger.Mutation, force = true)
        }
        return result
    }

    suspend fun updateEvent(event: BackendService.EventItem): Boolean {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) return false
        val success = backendService.updateEvent(sourceUrl, event)
        if (success) {
            refresh(RefreshTrigger.Mutation, force = true)
        }
        return success
    }

    suspend fun deleteEvents(rowNumbers: List<Int>): Boolean {
        val sourceUrl = currentSourceUrl.ifBlank { settingsManager.googleSheetUrl.first() }
        if (sourceUrl.isBlank()) return false
        val success = backendService.deleteEvents(sourceUrl, rowNumbers)
        if (success) {
            refresh(RefreshTrigger.Mutation, force = true)
        }
        return success
    }

    suspend fun refreshFromWorker(): Boolean {
        setSourceUrl(settingsManager.googleSheetUrl.first())
        return refresh(RefreshTrigger.Worker, force = true).success
    }

    private suspend fun loadCachedState(sourceUrl: String) {
        val cached = statsCache.loadState(sourceUrl)
        _uiState.value = _uiState.value.copy(
            currentSourceUrl = sourceUrl,
            homeSnapshot = resolveHomeSnapshot(cached.homeSnapshot, cached.eventsSnapshot),
            eventsSnapshot = cached.eventsSnapshot,
            isInitializing = false,
            isRefreshing = false,
            lastAttemptAt = cached.lastAttemptAt,
            lastSuccessAt = cached.lastSuccessAt,
        )
    }

    private fun resolveHomeSnapshot(
        homeSnapshot: CachedHomeSnapshot?,
        eventsSnapshot: CachedEventsSnapshot?,
        nowMs: Long = System.currentTimeMillis(),
    ): CachedHomeSnapshot? {
        val currentBabyDayStart = BabyDay.babyDayStartContaining(nowMs, ZoneId.systemDefault())
        return when {
            eventsSnapshot != null -> {
                CachedHomeSnapshot(
                    sourceUrl = eventsSnapshot.sourceUrl,
                    babyDayStartMs = currentBabyDayStart,
                    stats = StatsAggregation.homeStatsFromEvents(
                        allEvents = eventsSnapshot.events,
                        babyDayStartMs = currentBabyDayStart,
                        zone = ZoneId.systemDefault(),
                        nowMs = nowMs,
                    ),
                    lastSyncedAt = homeSnapshot?.lastSyncedAt ?: eventsSnapshot.lastSyncedAt,
                )
            }
            else -> homeSnapshot
        }
    }

    private fun resolveHomeStats(
        networkStats: BackendService.TodayStats?,
        cachedEvents: List<BackendService.EventItem>,
        nowMs: Long,
    ): BackendService.TodayStats? {
        val eventDerivedStats = if (cachedEvents.isNotEmpty()) {
            StatsAggregation.homeStatsFromEvents(
                allEvents = cachedEvents,
                babyDayStartMs = BabyDay.babyDayStartContaining(nowMs, ZoneId.systemDefault()),
                zone = ZoneId.systemDefault(),
                nowMs = nowMs,
            )
        } else {
            null
        }

        return when {
            networkStats != null && eventDerivedStats != null -> eventDerivedStats.copy(
                lastFeedTimeISO = eventDerivedStats.lastFeedTimeISO ?: networkStats.lastFeedTimeISO,
                lastPeeTimeISO = eventDerivedStats.lastPeeTimeISO ?: networkStats.lastPeeTimeISO,
                lastPoopTimeISO = eventDerivedStats.lastPoopTimeISO ?: networkStats.lastPoopTimeISO,
                lastSleepEventType = eventDerivedStats.lastSleepEventType ?: networkStats.lastSleepEventType,
                lastSleepEventTimeISO = eventDerivedStats.lastSleepEventTimeISO ?: networkStats.lastSleepEventTimeISO,
                previousWakeWindowLabel = eventDerivedStats.previousWakeWindowLabel ?: networkStats.previousWakeWindowLabel,
                previousSleepDurationLabel = eventDerivedStats.previousSleepDurationLabel ?: networkStats.previousSleepDurationLabel,
            )
            networkStats != null -> networkStats
            else -> eventDerivedStats
        }
    }
}

object BabySyncScheduler {
    private const val PERIODIC_WORK_NAME = "baby_periodic_refresh"

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<BabyRefreshWorker>(
            SyncConfig.PERIODIC_REFRESH_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

class BabyRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = BabyRepository(applicationContext)
        return if (repository.refreshFromWorker()) Result.success() else Result.retry()
    }
}
