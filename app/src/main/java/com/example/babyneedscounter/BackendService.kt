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
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                
                val request = Request.Builder()
                    .url("$webAppUrl?date=$today")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string() ?: "{}")
                    TodayStats(
                        totalEvents = jsonResponse.optInt("totalEvents", 0),
                        lastEventTime = jsonResponse.optString("lastEventTime", "—"),
                        poopPeeCount = jsonResponse.optInt("poopPeeCount", 0),
                        peeCount = jsonResponse.optInt("peeCount", 0),
                        feedCount = jsonResponse.optInt("feedCount", 0)
                    )
                } else {
                    Log.e("BackendService", "Failed to fetch stats: ${response.code}")
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
        val totalEvents: Int,
        val lastEventTime: String,
        val poopPeeCount: Int,
        val peeCount: Int,
        val feedCount: Int
    )
    
    companion object {
        fun getCurrentTimestamp(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }
}
