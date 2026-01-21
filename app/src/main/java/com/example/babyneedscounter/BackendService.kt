package com.example.babyneedscounter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BackendService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val statsCache = StatsCache(context)
    
    data class BabyEvent(
        val timestamp: String,
        val type: String, // "poop_pee", "pee", "feed"
        val notes: String = ""
    )
    
    data class EventItem(
        val rowNumber: Int,
        val timestamp: String, // ISO format from server
        val type: String,
        val notes: String
    )
    
    suspend fun logEvent(googleSheetUrl: String, event: BabyEvent): Boolean {
        if (googleSheetUrl.isEmpty()) {
            Log.e("BackendService", "Google Sheet URL not configured")
            return false
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
                
                if (success) {
                    Log.d("BackendService", "Successfully logged event: ${event.type}")
                    Log.d("BackendService", "Response: $responseBody")
                } else {
                    Log.e("BackendService", "Failed to log event. Status code: ${response.code}")
                    Log.e("BackendService", "Response body: $responseBody")
                    Log.e("BackendService", "Response message: ${response.message}")
                }
                
                response.close()
                success
            } catch (e: Exception) {
                Log.e("BackendService", "Error logging event: ${e.javaClass.simpleName}")
                Log.e("BackendService", "Error message: ${e.message}")
                e.printStackTrace()
                false
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
                
                // Calculate 7 AM to 7 AM window (baby day)
                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                
                // If it's before 7 AM, use yesterday at 7 AM as the start time
                if (currentHour < 7) {
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
                }
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 7)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                
                val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(calendar.time)
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
                    
                    val stats = TodayStats(
                        peeCount = jsonResponse.optInt("peeCount", 0),
                        poopCount = jsonResponse.optInt("poopCount", 0),
                        lastFeedTimeISO = lastFeedTimeISO
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
        val lastFeedTimeISO: String?
    ) {
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
    
    suspend fun fetchTodayEvents(googleSheetUrl: String): List<EventItem>? {
        if (googleSheetUrl.isEmpty()) {
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                
                // Calculate 7 AM to 7 AM window (baby day)
                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                
                // If it's before 7 AM, use yesterday at 7 AM as the start time
                if (currentHour < 7) {
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
                }
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 7)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                
                val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(calendar.time)
                val urlWithParams = "$webAppUrl?action=getEvents&startTime=$startTime"
                
                val request = Request.Builder()
                    .url(urlWithParams)
                    .get()
                    .build()
                
                Log.d("BackendService", "Fetching events from: $urlWithParams")
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
