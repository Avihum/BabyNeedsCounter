package com.example.babyneedscounter

import java.time.ZoneId
import kotlin.math.abs

data class InsightsFetchWindow(
    val startMs: Long,
    val endMs: Long,
)

sealed interface InsightsRow {
    val sortTimeMs: Long
    val stableKey: String

    data class RawEvent(val event: BackendService.EventItem, override val sortTimeMs: Long) : InsightsRow {
        override val stableKey: String = "raw_${event.rowNumber}"
    }

    data class CompletedSleepSession(
        val startEvent: BackendService.EventItem,
        val endEvent: BackendService.EventItem,
        val durationMinutes: Int,
        override val sortTimeMs: Long,
    ) : InsightsRow {
        override val stableKey: String = "sleep_${startEvent.rowNumber}_${endEvent.rowNumber}"
    }

    data class ActiveSleep(
        val startEvent: BackendService.EventItem,
        val elapsedMinutes: Int,
        override val sortTimeMs: Long,
    ) : InsightsRow {
        override val stableKey: String = "active_sleep_${startEvent.rowNumber}"
    }
}

/**
 * Aggregations for Insights: **7am–7am baby days**, sleep/wake from 😴/☀️ pairs, feeds/diapers by event timestamp.
 */
object StatsAggregation {

    data class SleepDayStatsPlaceholder(
        val totalSleepMinutes: Int?,
        val longestSleepMinutes: Int?,
        val wakeWindowSummaries: List<String>
    )

    data class FeedingDayStatsPlaceholder(
        val feedCount: Int,
        val intervalMinutesBetweenFeeds: List<Long>
    )

    data class DiaperDayStatsPlaceholder(
        val diaperCount: Int,
        val peeOnlyCount: Int,
        val poopRelatedCount: Int
    )

    /** Single day bundle for Insights cards (same layout for Today / Yesterday). */
    data class InsightDayBundle(
        val sleep: SleepDayStatsPlaceholder,
        val feed: FeedingDayStatsPlaceholder,
        val diaper: DiaperDayStatsPlaceholder
    )

    fun insightsFetchWindow(nowMs: Long, zone: ZoneId): InsightsFetchWindow {
        val currentBabyDayStart = BabyDay.babyDayStartContaining(nowMs, zone)
        return InsightsFetchWindow(
            startMs = BabyDay.offsetBabyDayStartMs(currentBabyDayStart, zone, -4),
            endMs = BabyDay.nextBabyDayStartMs(currentBabyDayStart, zone),
        )
    }

    fun insightBundleForBabyDay(
        allEventsChronological: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        capFeedDiaperLogAtNow: Boolean
    ): InsightDayBundle {
        val now = System.currentTimeMillis()
        val dayEvents = eventsForBabyDay(allEventsChronological, babyDayStartMs, zone, capFeedDiaperLogAtNow, now)
        return InsightDayBundle(
            sleep = sleepStatsForBabyDay(allEventsChronological, babyDayStartMs, zone),
            feed = feedingStatsFromEvents(dayEvents),
            diaper = diaperStatsFromEvents(dayEvents)
        )
    }

    fun homeStatsFromEvents(
        allEvents: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        nowMs: Long = System.currentTimeMillis(),
    ): BackendService.TodayStats {
        val currentDayEvents = eventsForBabyDay(allEvents, babyDayStartMs, zone, capAtNow = true, nowMs = nowMs)
        val currentDayTimeline = currentDayEvents.asTimedEvents(nowMs)
        val timeline = allEvents.asTimedEvents(nowMs)

        val latestFeed = currentDayTimeline.lastOrNull { it.event.type.contains("🐄") }?.event?.timestamp
        val latestPee = currentDayTimeline.lastOrNull { it.event.type.contains("💧") }?.event?.timestamp
        val latestPoop = currentDayTimeline.lastOrNull { it.event.type.contains("💩") }?.event?.timestamp
        val latestSleepMarker = timeline.lastOrNull {
            SheetEventMarkers.isSleepStart(it.event.type) || SheetEventMarkers.isSleepEnd(it.event.type)
        }?.event

        var previousWakeWindowLabel: String? = null
        var previousSleepDurationLabel: String? = null
        var wakeStartMs: Long? = null
        var sleepStartMs: Long? = null

        timeline.forEach { timedEvent ->
            val event = timedEvent.event
            val timeMs = timedEvent.timeMs
            when {
                SheetEventMarkers.isSleepStart(event.type) -> {
                    if (wakeStartMs != null && timeMs > wakeStartMs!!) {
                        previousWakeWindowLabel = formatShortDurationMinutes(((timeMs - wakeStartMs!!) / 60_000L).toInt())
                    }
                    wakeStartMs = null
                    sleepStartMs = timeMs
                }
                SheetEventMarkers.isSleepEnd(event.type) -> {
                    if (sleepStartMs != null && timeMs > sleepStartMs!!) {
                        previousSleepDurationLabel = formatShortDurationMinutes(((timeMs - sleepStartMs!!) / 60_000L).toInt())
                    }
                    sleepStartMs = null
                    wakeStartMs = timeMs
                }
            }
        }

        return BackendService.TodayStats(
            peeCount = currentDayEvents.count { it.type.contains("💧") },
            poopCount = currentDayEvents.count { it.type.contains("💩") },
            feedCount = currentDayEvents.count { it.type.contains("🐄") },
            lastFeedTimeISO = latestFeed,
            lastPeeTimeISO = latestPee,
            lastPoopTimeISO = latestPoop,
            lastSleepEventType = latestSleepMarker?.type,
            lastSleepEventTimeISO = latestSleepMarker?.timestamp,
            previousWakeWindowLabel = previousWakeWindowLabel,
            previousSleepDurationLabel = previousSleepDurationLabel,
        )
    }

    fun insightsRowsForBabyDay(
        allEventsChronological: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        capAtNow: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): List<InsightsRow> {
        val allTimedEventsAsc = allEventsChronological.asTimedEvents(
            maxTimeMs = if (capAtNow) nowMs else Long.MAX_VALUE
        )
        val dayEventsAsc = eventsForBabyDay(allEventsChronological, babyDayStartMs, zone, capAtNow, nowMs)
            .sortedBy { parseEventTimeMs(it.timestamp) ?: Long.MAX_VALUE }

        val rows = mutableListOf<InsightsRow>()
        var pendingSleepStart: BackendService.EventItem? = null

        for (event in dayEventsAsc) {
            val eventTimeMs = parseEventTimeMs(event.timestamp) ?: continue
            when {
                SheetEventMarkers.isSleepStart(event.type) -> {
                    pendingSleepStart?.let { previousStart ->
                        val previousStartTime = parseEventTimeMs(previousStart.timestamp) ?: eventTimeMs
                        rows += InsightsRow.RawEvent(previousStart, previousStartTime)
                    }
                    pendingSleepStart = event
                }

                SheetEventMarkers.isSleepEnd(event.type) -> {
                    val startEvent = pendingSleepStart
                    val startTimeMs = startEvent?.let { parseEventTimeMs(it.timestamp) }
                    if (startEvent != null && startTimeMs != null && eventTimeMs > startTimeMs) {
                        rows += InsightsRow.CompletedSleepSession(
                            startEvent = startEvent,
                            endEvent = event,
                            durationMinutes = ((eventTimeMs - startTimeMs) / 60_000L).toInt(),
                            sortTimeMs = eventTimeMs,
                        )
                        pendingSleepStart = null
                    } else {
                        if (startEvent != null) {
                            rows += InsightsRow.RawEvent(startEvent, startTimeMs ?: eventTimeMs)
                            pendingSleepStart = null
                        }
                        rows += InsightsRow.RawEvent(event, eventTimeMs)
                    }
                }

                else -> rows += InsightsRow.RawEvent(event, eventTimeMs)
            }
        }

        pendingSleepStart?.let { startEvent ->
            val startTimeMs = parseEventTimeMs(startEvent.timestamp) ?: nowMs
            val closure = findSleepClosure(startEvent, startTimeMs, allTimedEventsAsc)
            if (closure != null && closure.timeMs > startTimeMs) {
                rows += InsightsRow.CompletedSleepSession(
                    startEvent = startEvent,
                    endEvent = closure.event,
                    durationMinutes = ((closure.timeMs - startTimeMs) / 60_000L).toInt(),
                    sortTimeMs = closure.timeMs,
                )
            } else {
                rows += InsightsRow.ActiveSleep(
                    startEvent = startEvent,
                    elapsedMinutes = ((nowMs - startTimeMs) / 60_000L).toInt().coerceAtLeast(0),
                    sortTimeMs = startTimeMs,
                )
            }
        }

        return rows.sortedByDescending { it.sortTimeMs }
    }

    fun feedingStatsFromEvents(events: List<BackendService.EventItem>): FeedingDayStatsPlaceholder {
        val feeds = events.filter { it.type.contains("🐄") }
            .mapNotNull { parseEventTimeMs(it.timestamp) }
            .sorted()
        val intervals = mutableListOf<Long>()
        for (i in 1 until feeds.size) {
            intervals.add((feeds[i] - feeds[i - 1]) / 60_000L)
        }
        return FeedingDayStatsPlaceholder(
            feedCount = feeds.size,
            intervalMinutesBetweenFeeds = intervals
        )
    }

    fun diaperStatsFromEvents(events: List<BackendService.EventItem>): DiaperDayStatsPlaceholder {
        val diaperEvents = events.filter { it.type.contains("💧") || it.type.contains("💩") }
        return DiaperDayStatsPlaceholder(
            diaperCount = diaperEvents.size,
            peeOnlyCount = events.count { it.type == "💧" },
            poopRelatedCount = events.count { it.type.contains("💩") }
        )
    }

    /**
     * Events in the baby day **[babyDayStartMs, next 7am)**.
     * If [capAtNow] and this interval is the **current** baby day, drops events after [nowMs].
     */
    fun eventsForBabyDay(
        events: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        capAtNow: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): List<BackendService.EventItem> {
        val endExclusive = BabyDay.nextBabyDayStartMs(babyDayStartMs, zone)
        val currentBabyDayStart = BabyDay.babyDayStartContaining(nowMs, zone)
        return events.mapNotNull { e ->
            val t = parseEventTimeMs(e.timestamp) ?: return@mapNotNull null
            if (t < babyDayStartMs || t >= endExclusive) return@mapNotNull null
            if (capAtNow && babyDayStartMs == currentBabyDayStart && t > nowMs) return@mapNotNull null
            e
        }.sortedByDescending { parseEventTimeMs(it.timestamp) ?: 0L }
    }

    /** Formats average interval: minutes only if under 60, else "Xh Ym" / "Xh". */
    fun formatAverageFeedIntervalMinutes(avgMinutes: Int): String {
        if (avgMinutes < 60) return "${avgMinutes}m"
        val h = avgMinutes / 60
        val m = avgMinutes % 60
        return if (m > 0) "${h}h ${m}m" else "${h}h"
    }

    /**
     * Sleep totals and wake windows for the baby day starting at [babyDayStartMs]:
     * sleep credited when 😴 falls in **[start, next 7am)**; wake window when ☀️ falls in that range.
     */
    fun sleepStatsForBabyDay(
        allEventsSortedOrNot: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId
    ): SleepDayStatsPlaceholder {
        val endExclusive = BabyDay.nextBabyDayStartMs(babyDayStartMs, zone)
        val sorted = allEventsSortedOrNot.sortedBy { parseEventTimeMs(it.timestamp) ?: 0L }
        var sleepStartMs: Long? = null
        var wakeStartMs: Long? = null
        var totalMsForDay = 0L
        var longestMsForDay = 0L
        val wakeDurationsForDay = mutableListOf<Long>()

        fun inDay(t: Long) = t >= babyDayStartMs && t < endExclusive

        for (event in sorted) {
            val t = parseEventTimeMs(event.timestamp) ?: continue
            when {
                SheetEventMarkers.isSleepStart(event.type) -> {
                    if (wakeStartMs != null) {
                        val ww = t - wakeStartMs
                        if (ww > 0 && inDay(wakeStartMs)) {
                            wakeDurationsForDay.add(ww)
                        }
                    }
                    wakeStartMs = null
                    sleepStartMs = t
                }
                SheetEventMarkers.isSleepEnd(event.type) -> {
                    if (sleepStartMs != null) {
                        val duration = t - sleepStartMs
                        if (duration > 0 && inDay(sleepStartMs)) {
                            totalMsForDay += duration
                            if (duration > longestMsForDay) longestMsForDay = duration
                        }
                        sleepStartMs = null
                    }
                    wakeStartMs = t
                }
            }
        }

        val totalMin = if (totalMsForDay > 0) (totalMsForDay / 60_000L).toInt() else null
        val longestMin = if (longestMsForDay > 0) (longestMsForDay / 60_000L).toInt() else null
        val wakeSummaries = wakeDurationsForDay
            .map { formatShortDurationMinutes((it / 60_000L).toInt()) }
            .filter { it.isNotEmpty() }

        return SleepDayStatsPlaceholder(
            totalSleepMinutes = totalMin,
            longestSleepMinutes = longestMin,
            wakeWindowSummaries = wakeSummaries
        )
    }

    fun formatShortDurationMinutes(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /** Signed delta for sleep: anchor − compare (e.g. current baby day − previous). */
    fun formatSleepDeltaMinutes(anchorMinutes: Int?, compareMinutes: Int?): String {
        val a = anchorMinutes ?: 0
        val b = compareMinutes ?: 0
        val delta = a - b
        if (delta == 0) return "0m"
        val sign = if (delta > 0) "+" else "-"
        val body = formatShortDurationMinutes(abs(delta))
        return "$sign$body"
    }

    fun formatCountDelta(anchor: Int, compare: Int): String {
        val delta = anchor - compare
        return when {
            delta > 0 -> "+$delta"
            delta < 0 -> "$delta"
            else -> "0"
        }
    }

    private data class TimedEvent(val event: BackendService.EventItem, val timeMs: Long)

    private fun findSleepClosure(
        startEvent: BackendService.EventItem,
        startTimeMs: Long,
        allTimedEventsAsc: List<TimedEvent>,
    ): TimedEvent? {
        var passedStart = false
        for (timedEvent in allTimedEventsAsc) {
            if (!passedStart) {
                if (timedEvent.event.rowNumber == startEvent.rowNumber && timedEvent.timeMs == startTimeMs) {
                    passedStart = true
                }
                continue
            }

            when {
                SheetEventMarkers.isSleepEnd(timedEvent.event.type) -> return timedEvent
                SheetEventMarkers.isSleepStart(timedEvent.event.type) -> return null
            }
        }
        return null
    }

    private fun List<BackendService.EventItem>.asTimedEvents(maxTimeMs: Long): List<TimedEvent> {
        return mapNotNull { event ->
            val timeMs = parseEventTimeMs(event.timestamp) ?: return@mapNotNull null
            if (timeMs > maxTimeMs) return@mapNotNull null
            TimedEvent(event, timeMs)
        }.sortedBy { it.timeMs }
    }

    fun parseEventTimeMs(iso: String): Long? = ServerDateTimes.parseMillis(iso)
}
