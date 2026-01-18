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
    
    data class BabyEvent(
        val timestamp: String,
        val type: String, // "poop_pee", "pee", "feed"
        val notes: String = ""
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
    
    suspend fun fetchTodayStats(googleSheetUrl: String): TodayStats? {
        if (googleSheetUrl.isEmpty()) {
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val webAppUrl = convertToWebAppUrl(googleSheetUrl)
                
                // TEMPORARY: Get ALL events (no filtering) for debugging
                val request = Request.Builder()
                    .url(webAppUrl)
                    .get()
                    .build()
                
                Log.d("BackendService", "Fetching stats from: $webAppUrl (ALL EVENTS - no filter for debugging)")
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
                    stats
                } else {
                    Log.e("BackendService", "Failed to fetch stats: ${response.code}")
                    Log.e("BackendService", "Response body: ${response.body?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("BackendService", "Error fetching stats", e)
                null
            }
        }
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
    }
    
    companion object {
        fun getCurrentTimestamp(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        }
    }
}
