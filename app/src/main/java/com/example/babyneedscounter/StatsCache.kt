package com.example.babyneedscounter

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.statsCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "stats_cache")

data class CachedHomeSnapshot(
    val sourceUrl: String,
    val babyDayStartMs: Long,
    val stats: BackendService.TodayStats,
    val lastSyncedAt: Long,
)

data class CachedEventsSnapshot(
    val sourceUrl: String,
    val fetchStartMs: Long,
    val fetchEndMs: Long,
    val events: List<BackendService.EventItem>,
    val lastSyncedAt: Long,
)

data class CachedRepositoryState(
    val homeSnapshot: CachedHomeSnapshot? = null,
    val eventsSnapshot: CachedEventsSnapshot? = null,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
)

class StatsCache(private val context: Context) {

    companion object {
        private val HOME_SNAPSHOT_KEY = stringPreferencesKey("home_snapshot_json")
        private val EVENTS_SNAPSHOT_KEY = stringPreferencesKey("events_snapshot_json")
        private val LAST_ATTEMPT_AT_KEY = longPreferencesKey("last_attempt_at")
        private val LAST_SUCCESS_AT_KEY = longPreferencesKey("last_success_at")
    }

    suspend fun loadState(sourceUrl: String): CachedRepositoryState {
        return try {
            val preferences = context.statsCacheDataStore.data.first()
            CachedRepositoryState(
                homeSnapshot = preferences[HOME_SNAPSHOT_KEY]
                    ?.let(::JSONObject)
                    ?.takeIf { it.optString("sourceUrl") == sourceUrl }
                    ?.toCachedHomeSnapshot(),
                eventsSnapshot = preferences[EVENTS_SNAPSHOT_KEY]
                    ?.let(::JSONObject)
                    ?.takeIf { it.optString("sourceUrl") == sourceUrl }
                    ?.toCachedEventsSnapshot(),
                lastAttemptAt = preferences[LAST_ATTEMPT_AT_KEY],
                lastSuccessAt = preferences[LAST_SUCCESS_AT_KEY],
            )
        } catch (e: Exception) {
            Log.e("StatsCache", "Error loading cached repository state", e)
            CachedRepositoryState()
        }
    }

    suspend fun saveState(
        homeSnapshot: CachedHomeSnapshot?,
        eventsSnapshot: CachedEventsSnapshot?,
        lastAttemptAt: Long?,
        lastSuccessAt: Long?,
    ) {
        try {
            context.statsCacheDataStore.edit { preferences ->
                if (homeSnapshot != null) {
                    preferences[HOME_SNAPSHOT_KEY] = homeSnapshot.toJson().toString()
                } else {
                    preferences.remove(HOME_SNAPSHOT_KEY)
                }

                if (eventsSnapshot != null) {
                    preferences[EVENTS_SNAPSHOT_KEY] = eventsSnapshot.toJson().toString()
                } else {
                    preferences.remove(EVENTS_SNAPSHOT_KEY)
                }

                if (lastAttemptAt != null) {
                    preferences[LAST_ATTEMPT_AT_KEY] = lastAttemptAt
                } else {
                    preferences.remove(LAST_ATTEMPT_AT_KEY)
                }

                if (lastSuccessAt != null) {
                    preferences[LAST_SUCCESS_AT_KEY] = lastSuccessAt
                } else {
                    preferences.remove(LAST_SUCCESS_AT_KEY)
                }
            }
        } catch (e: Exception) {
            Log.e("StatsCache", "Error saving cached repository state", e)
        }
    }

    suspend fun clear() {
        try {
            context.statsCacheDataStore.edit { it.clear() }
        } catch (e: Exception) {
            Log.e("StatsCache", "Error clearing cache", e)
        }
    }
}

private fun CachedHomeSnapshot.toJson(): JSONObject = JSONObject().apply {
    put("sourceUrl", sourceUrl)
    put("babyDayStartMs", babyDayStartMs)
    put("lastSyncedAt", lastSyncedAt)
    put("stats", stats.toJson())
}

private fun CachedEventsSnapshot.toJson(): JSONObject = JSONObject().apply {
    put("sourceUrl", sourceUrl)
    put("fetchStartMs", fetchStartMs)
    put("fetchEndMs", fetchEndMs)
    put("lastSyncedAt", lastSyncedAt)
    put(
        "events",
        JSONArray().apply {
            events.forEach { put(it.toJson()) }
        }
    )
}

private fun BackendService.TodayStats.toJson(): JSONObject = JSONObject().apply {
    put("peeCount", peeCount)
    put("poopCount", poopCount)
    put("feedCount", feedCount)
    putNullable("lastFeedTimeISO", lastFeedTimeISO)
    putNullable("lastPeeTimeISO", lastPeeTimeISO)
    putNullable("lastPoopTimeISO", lastPoopTimeISO)
    putNullable("lastSleepEventType", lastSleepEventType)
    putNullable("lastSleepEventTimeISO", lastSleepEventTimeISO)
    putNullable("previousWakeWindowLabel", previousWakeWindowLabel)
    putNullable("previousSleepDurationLabel", previousSleepDurationLabel)
}

private fun BackendService.EventItem.toJson(): JSONObject = JSONObject().apply {
    put("rowNumber", rowNumber)
    put("timestamp", timestamp)
    put("type", type)
    put("notes", notes)
}

private fun JSONObject.toCachedHomeSnapshot(): CachedHomeSnapshot {
    val statsJson = getJSONObject("stats")
    return CachedHomeSnapshot(
        sourceUrl = getString("sourceUrl"),
        babyDayStartMs = getLong("babyDayStartMs"),
        stats = BackendService.TodayStats(
            peeCount = statsJson.optInt("peeCount"),
            poopCount = statsJson.optInt("poopCount"),
            feedCount = statsJson.optInt("feedCount"),
            lastFeedTimeISO = statsJson.optNullableString("lastFeedTimeISO"),
            lastPeeTimeISO = statsJson.optNullableString("lastPeeTimeISO"),
            lastPoopTimeISO = statsJson.optNullableString("lastPoopTimeISO"),
            lastSleepEventType = statsJson.optNullableString("lastSleepEventType"),
            lastSleepEventTimeISO = statsJson.optNullableString("lastSleepEventTimeISO"),
            previousWakeWindowLabel = statsJson.optNullableString("previousWakeWindowLabel"),
            previousSleepDurationLabel = statsJson.optNullableString("previousSleepDurationLabel"),
        ),
        lastSyncedAt = getLong("lastSyncedAt"),
    )
}

private fun JSONObject.toCachedEventsSnapshot(): CachedEventsSnapshot {
    val array = optJSONArray("events") ?: JSONArray()
    val events = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                BackendService.EventItem(
                    rowNumber = item.getInt("rowNumber"),
                    timestamp = item.getString("timestamp"),
                    type = item.getString("type"),
                    notes = item.optString("notes", ""),
                )
            )
        }
    }

    return CachedEventsSnapshot(
        sourceUrl = getString("sourceUrl"),
        fetchStartMs = getLong("fetchStartMs"),
        fetchEndMs = getLong("fetchEndMs"),
        events = events,
        lastSyncedAt = getLong("lastSyncedAt"),
    )
}

private fun JSONObject.putNullable(key: String, value: String?) {
    if (value == null) {
        put(key, JSONObject.NULL)
    } else {
        put(key, value)
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    return if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
