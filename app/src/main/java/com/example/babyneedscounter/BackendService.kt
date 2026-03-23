package com.example.babyneedscounter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Google Sheet column B markers for sleep (emoji-only for new rows).
 * [SLEEP_STARTED]=😴 fell asleep; [SLEEP_ENDED]=☀️ woke up — distinct from 🐄 💩 💧.
 */
object SheetEventMarkers {
    const val SLEEP_STARTED = "😴"
    const val SLEEP_ENDED = "☀️"
}

class BackendService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val statsCache = StatsCache(context)
    
    data class BabyEvent(
        val timestamp: String,
        /** Sheet column B: emoji markers (e.g. 🐄 💧 😴 ☀️). */
        val type: String,
        val notes: String = ""
    )
    
    data class EventItem(
        val rowNumber: Int,
        val timestamp: String, // ISO format from server
        val type: String,
        val notes: String
    )
    
    data class LogEventResult(
        val success: Boolean,
        val rowNumber: Int? = null
    )
    
    suspend fun logEvent(googleSheetUrl: String, event: BabyEvent): LogEventResult {
        if (googleSheetUrl.isEmpty()) {
            Log.e("BackendService", "Google Sheet URL not configured")
            return LogEventResult(success = false)
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Convert the Google Sheets URL to the Web App URL format
                // Expected format: https://script.google.com/macros/s/{SCRIPT_ID}/exec
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                Log.d("BackendService", "Using URL: $webAppUrl")
                
                val json = JSONObject().apply {
                    put("timestamp", event.timestamp)
                    put("type", event.type)
                    put("notes", event.notes)
                }
                
                Log.d("BackendService", "Sending JSON: ${json.toString()}")
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(requestBody)
                    .build()
                
                Log.d("BackendService", "Making HTTP POST request...")
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                val responseBody = response.body?.string() ?: ""
                
                val rowNumber: Int? = if (success && responseBody.isNotEmpty()) {
                    try {
                        val respJson = JSONObject(responseBody)
                        if (respJson.optString("status") == "success" && respJson.has("rowNumber") && !respJson.isNull("rowNumber")) {
                            val n = respJson.getInt("rowNumber")
                            if (n > 0) n else null
                        } else null
                    } catch (e: Exception) {
                        Log.w("BackendService", "Could not parse rowNumber from log response", e)
                        null
                    }
                } else null
                
                if (success) {
                    Log.d("BackendService", "Successfully logged event: ${event.type}, rowNumber=$rowNumber")
                    Log.d("BackendService", "Response: $responseBody")
                } else {
                    Log.e("BackendService", "Failed to log event. Status code: ${response.code}")
                    Log.e("BackendService", "Response body: $responseBody")
                    Log.e("BackendService", "Response message: ${response.message}")
                }
                
                response.close()
                LogEventResult(success = success, rowNumber = rowNumber)
            } catch (e: Exception) {
                Log.e("BackendService", "Error logging event: ${e.javaClass.simpleName}")
                Log.e("BackendService", "Error message: ${e.message}")
                e.printStackTrace()
                LogEventResult(success = false)
            }
        }
    }
    
    suspend fun fetchTodayStats(googleSheetUrl: String, useCache: Boolean = true): TodayStats? {
        if (googleSheetUrl.isEmpty()) {
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)

                val zone = ZoneId.systemDefault()
                val startTime = BabyDay.formatSheetStartTime(
                    BabyDay.babyDayStartContaining(System.currentTimeMillis(), zone),
                    zone
                )
                val urlWithParams = "$webAppUrl?startTime=$startTime"
                
                val request = Request.Builder()
                    .url(urlWithParams)
                    .get()
                    .build()
                
                Log.d("BackendService", "Fetching stats from: $urlWithParams (filtering from 7 AM)")
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    Log.d("BackendService", "Stats response: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    
                    // Log debug info if available
                    if (jsonResponse.has("debug")) {
                        val debug = jsonResponse.getJSONObject("debug")
                        Log.d("BackendService", "Debug info: ${debug.toString()}")
                    }
                    
                    val lastFeedTimeISO = if (jsonResponse.has("lastFeedTimeISO") && !jsonResponse.isNull("lastFeedTimeISO")) {
                        jsonResponse.getString("lastFeedTimeISO")
                    } else {
                        null
                    }
                    
                    val lastSleepEventType = if (jsonResponse.has("lastSleepEventType") && !jsonResponse.isNull("lastSleepEventType")) {
                        jsonResponse.getString("lastSleepEventType")
                    } else {
                        null
                    }
                    
                    val lastSleepEventTimeISO = if (jsonResponse.has("lastSleepEventTimeISO") && !jsonResponse.isNull("lastSleepEventTimeISO")) {
                        jsonResponse.getString("lastSleepEventTimeISO")
                    } else {
                        null
                    }
                    
                    val lastPeeTimeISO = if (jsonResponse.has("lastPeeTimeISO") && !jsonResponse.isNull("lastPeeTimeISO")) {
                        jsonResponse.getString("lastPeeTimeISO")
                    } else {
                        null
                    }
                    val lastPoopTimeISO = if (jsonResponse.has("lastPoopTimeISO") && !jsonResponse.isNull("lastPoopTimeISO")) {
                        jsonResponse.getString("lastPoopTimeISO")
                    } else {
                        null
                    }

                    val previousWakeWindowLabel =
                        if (jsonResponse.has("previousWakeWindowLabel") && !jsonResponse.isNull("previousWakeWindowLabel")) {
                            jsonResponse.getString("previousWakeWindowLabel").takeIf { it.isNotBlank() }
                        } else {
                            null
                        }

                    val stats = TodayStats(
                        peeCount = jsonResponse.optInt("peeCount", 0),
                        poopCount = jsonResponse.optInt("poopCount", 0),
                        feedCount = jsonResponse.optInt("feedCount", 0),
                        lastFeedTimeISO = lastFeedTimeISO,
                        lastPeeTimeISO = lastPeeTimeISO,
                        lastPoopTimeISO = lastPoopTimeISO,
                        lastSleepEventType = lastSleepEventType,
                        lastSleepEventTimeISO = lastSleepEventTimeISO,
                        previousWakeWindowLabel = previousWakeWindowLabel
                    )
                    
                    Log.d("BackendService", "Parsed stats: pee=${stats.peeCount}, poop=${stats.poopCount}, feed=${stats.getTimeSinceLastFeed()}, feedTimeISO=${stats.lastFeedTimeISO}")
                    
                    // Cache the stats for future use
                    if (useCache) {
                        statsCache.saveStats(stats)
                        Log.d("BackendService", "Stats saved to cache")
                    }
                    
                    stats
                } else {
                    Log.e("BackendService", "Failed to fetch stats: ${response.code}")
                    Log.e("BackendService", "Response body: ${response.body?.string()}")
                    
                    // Return cached stats if available
                    if (useCache) {
                        Log.d("BackendService", "Attempting to return cached stats due to fetch failure")
                        statsCache.getCachedStats()
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendService", "Error fetching stats", e)
                
                // Return cached stats if available
                if (useCache) {
                    Log.d("BackendService", "Attempting to return cached stats due to exception")
                    statsCache.getCachedStats()
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * Get cached stats without making a network call
     */
    suspend fun getCachedStats(): TodayStats? {
        return statsCache.getCachedStats()
    }
    
    private fun convertToWebAppUrl(url: String): String {
        // If it's already a web app URL, return as-is
        if (url.contains("/macros/s/")) {
            return url
        }
        
        // For now, return the URL as provided
        // User should provide the deployed web app URL
        return url
    }
    
    data class TodayStats(
        val peeCount: Int,
        val poopCount: Int,
        /** Feeds logged in the current baby day (🐄), from stats API. */
        val feedCount: Int = 0,
        val lastFeedTimeISO: String?,
        /** Most recent pee-related row today (column A as ISO). */
        val lastPeeTimeISO: String? = null,
        /** Most recent poop-related row today (column A as ISO). */
        val lastPoopTimeISO: String? = null,
        /** Raw column B value from stats: 😴 / ☀️ for new rows, or legacy sleep_started / sleep_ended. */
        val lastSleepEventType: String? = null,
        val lastSleepEventTimeISO: String? = null,
        /** Last completed wake window in baby day (☀️→😴), e.g. "1h 25m"; for UI when currently awake. */
        val previousWakeWindowLabel: String? = null
    ) {
        private fun isSleepStartMarker(): Boolean =
            lastSleepEventType == SheetEventMarkers.SLEEP_STARTED || lastSleepEventType == "sleep_started"
        
        private fun isSleepEndMarker(): Boolean =
            lastSleepEventType == SheetEventMarkers.SLEEP_ENDED || lastSleepEventType == "sleep_ended"
        
        fun isSleeping(): Boolean = isSleepStartMarker()
        
        /** Latest sleep-related row is a wake event (baby awake since then). */
        fun isAwakeFromLastSleepEvent(): Boolean = isSleepEndMarker()
        
        fun formatSleepStartClock(): String {
            if (lastSleepEventTimeISO == null || !isSleeping()) return "—"
            return formatIsoToClock(lastSleepEventTimeISO)
        }
        
        fun formatLastSleepEndedClock(): String {
            if (lastSleepEventTimeISO == null || !isSleepEndMarker()) return "—"
            return formatIsoToClock(lastSleepEventTimeISO)
        }
        
        /** Duration since fell asleep (😴 / sleep_started). */
        fun getSleepDurationLabel(): String {
            if (!isSleeping() || lastSleepEventTimeISO == null) return "—"
            return try {
                val start = parseIsoToDate(lastSleepEventTimeISO) ?: return "—"
                val diffMs = Date().time - start.time
                val diffMinutes = (diffMs / 60000).toInt().coerceAtLeast(0)
                val hours = diffMinutes / 60
                val minutes = diffMinutes % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } catch (e: Exception) {
                "—"
            }
        }
        
        /** Time awake since last wake (☀️ / sleep_ended). */
        fun getAwakeDurationLabel(): String {
            if (!isSleepEndMarker() || lastSleepEventTimeISO == null) return "—"
            return try {
                val wakeTime = parseIsoToDate(lastSleepEventTimeISO) ?: return "—"
                val diffMs = Date().time - wakeTime.time
                val diffMinutes = (diffMs / 60000).toInt().coerceAtLeast(0)
                val hours = diffMinutes / 60
                val minutes = diffMinutes % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } catch (e: Exception) {
                "—"
            }
        }
        
        private fun formatIsoToClock(iso: String): String {
            return try {
                val d = parseIsoToDate(iso) ?: return "—"
                SimpleDateFormat("HH:mm", Locale.US).format(d)
            } catch (e: Exception) {
                "—"
            }
        }

        /** Clock time (local) for last pee event today, or "—". */
        fun getLastPeeClock(): String {
            if (lastPeeTimeISO == null) return "—"
            return formatIsoToClock(lastPeeTimeISO)
        }

        /** Clock time (local) for last poop event today, or "—". */
        fun getLastPoopClock(): String {
            if (lastPoopTimeISO == null) return "—"
            return formatIsoToClock(lastPoopTimeISO)
        }

        /** Relative time since last pee today (e.g. "1h 20m", "just now"), or "—". */
        fun getLastPeeAgo(): String = relativeAgoFromIso(lastPeeTimeISO)

        /** Relative time since last poop today, or "—". */
        fun getLastPoopAgo(): String = relativeAgoFromIso(lastPoopTimeISO)

        private fun relativeAgoFromIso(iso: String?): String {
            if (iso == null) return "—"
            return try {
                val t = parseIsoToDate(iso) ?: return "—"
                val diffMs = Date().time - t.time
                val diffMinutes = (diffMs / 60000).toInt().coerceAtLeast(0)
                val hours = diffMinutes / 60
                val minutes = diffMinutes % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m"
                    else -> "just now"
                }
            } catch (e: Exception) {
                "—"
            }
        }
        
        private fun parseIsoToDate(iso: String): Date? {
            return try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(iso)
            } catch (e: Exception) {
                null
            }
        }
        fun getTimeSinceLastFeed(): String {
            if (lastFeedTimeISO == null) return "—"
            
            return try {
                val feedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(lastFeedTimeISO)
                
                if (feedTime == null) return "—"
                
                val now = Date()
                val diffMs = now.time - feedTime.time
                val diffMinutes = (diffMs / 60000).toInt()
                
                val hours = diffMinutes / 60
                val minutes = diffMinutes % 60
                
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m"
                    else -> "Just now"
                }
            } catch (e: Exception) {
                "—"
            }
        }
        
        fun getLastFeedTime(): String {
            if (lastFeedTimeISO == null) return "—"
            
            return try {
                val feedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(lastFeedTimeISO)
                
                if (feedTime == null) return "—"
                
                SimpleDateFormat("HH:mm", Locale.US).format(feedTime)
            } catch (e: Exception) {
                "—"
            }
        }
        
        fun getNextFeedTime(): String {
            if (lastFeedTimeISO == null) return "—"
            
            return try {
                val feedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(lastFeedTimeISO)
                
                if (feedTime == null) return "—"
                
                // Add 3 hours (in milliseconds)
                val nextFeedTime = Date(feedTime.time + (3 * 60 * 60 * 1000))
                
                SimpleDateFormat("HH:mm", Locale.US).format(nextFeedTime)
            } catch (e: Exception) {
                "—"
            }
        }
        
        fun getTimeUntilNextFeed(): String {
            if (lastFeedTimeISO == null) return "—"
            
            return try {
                val feedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(lastFeedTimeISO)
                
                if (feedTime == null) return "—"
                
                // Add 3 hours to get next feed time
                val nextFeedTime = Date(feedTime.time + (3 * 60 * 60 * 1000))
                val now = Date()
                
                val diffMs = nextFeedTime.time - now.time
                val diffMinutes = (diffMs / 60000).toInt()
                
                when {
                    diffMinutes <= 0 -> "Now!"
                    diffMinutes < 60 -> "${diffMinutes}m"
                    else -> {
                        val hours = diffMinutes / 60
                        val minutes = diffMinutes % 60
                        if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                    }
                }
            } catch (e: Exception) {
                "—"
            }
        }
    }
    
    /**
     * Fetches events for Insights: **7am–7am baby days** — from four baby days before the current one’s start
     * through the end of the **current** baby day (next 7am exclusive), so Today/Yesterday and prior-day comparisons have data.
     */
    suspend fun fetchInsightsEvents(googleSheetUrl: String): List<EventItem>? {
        if (googleSheetUrl.isEmpty()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                val zone = ZoneId.systemDefault()
                val now = System.currentTimeMillis()
                val currentBabyDayStart = BabyDay.babyDayStartContaining(now, zone)
                val startFetch = BabyDay.offsetBabyDayStartMs(currentBabyDayStart, zone, -4)
                val endExclusive = BabyDay.nextBabyDayStartMs(currentBabyDayStart, zone)
                val startTime = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(startFetch))
                val endTime = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(endExclusive))

                val base = webAppUrl.toHttpUrlOrNull() ?: return@withContext null
                val url = base.newBuilder()
                    .addQueryParameter("action", "getEvents")
                    .addQueryParameter("startTime", startTime)
                    .addQueryParameter("endTime", endTime)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                Log.d("BackendService", "Fetching insights events from: $url")
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    Log.d("BackendService", "Events response: $responseBody")
                    val jsonResponse = JSONObject(responseBody)
                    
                    if (jsonResponse.getString("status") == "success") {
                        val eventsArray = jsonResponse.getJSONArray("events")
                        val events = mutableListOf<EventItem>()
                        
                        for (i in 0 until eventsArray.length()) {
                            val eventJson = eventsArray.getJSONObject(i)
                            events.add(
                                EventItem(
                                    rowNumber = eventJson.getInt("rowNumber"),
                                    timestamp = eventJson.getString("timestamp"),
                                    type = eventJson.getString("type"),
                                    notes = eventJson.optString("notes", "")
                                )
                            )
                        }
                        
                        Log.d("BackendService", "Parsed ${events.size} events")
                        events
                    } else {
                        Log.e("BackendService", "Error in response: ${jsonResponse.optString("message")}")
                        null
                    }
                } else {
                    Log.e("BackendService", "Failed to fetch events: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e("BackendService", "Error fetching events", e)
                null
            }
        }
    }
    
    suspend fun updateEvent(googleSheetUrl: String, event: EventItem): Boolean {
        if (googleSheetUrl.isEmpty()) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                
                val json = JSONObject().apply {
                    put("action", "update")
                    put("rowNumber", event.rowNumber)
                    put("timestamp", event.timestamp)
                    put("type", event.type)
                    put("notes", event.notes)
                }
                
                Log.d("BackendService", "Updating event: ${json.toString()}")
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                val responseBody = response.body?.string() ?: ""
                
                if (success) {
                    Log.d("BackendService", "Successfully updated event")
                } else {
                    Log.e("BackendService", "Failed to update event: ${response.code} - $responseBody")
                }
                
                response.close()
                success
            } catch (e: Exception) {
                Log.e("BackendService", "Error updating event", e)
                false
            }
        }
    }
    
    suspend fun deleteEvents(googleSheetUrl: String, rowNumbers: List<Int>): Boolean {
        if (googleSheetUrl.isEmpty() || rowNumbers.isEmpty()) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                
                val json = JSONObject().apply {
                    put("action", "delete")
                    put("rowNumbers", JSONArray(rowNumbers))
                }
                
                Log.d("BackendService", "Deleting events: ${json.toString()}")
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                val responseBody = response.body?.string() ?: ""
                
                if (success) {
                    Log.d("BackendService", "Successfully deleted ${rowNumbers.size} event(s)")
                } else {
                    Log.e("BackendService", "Failed to delete events: ${response.code} - $responseBody")
                }
                
                response.close()
                success
            } catch (e: Exception) {
                Log.e("BackendService", "Error deleting events", e)
                false
            }
        }
    }
    
    companion object {
        fun getCurrentTimestamp(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        }
    }
}
