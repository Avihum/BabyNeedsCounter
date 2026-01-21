package com.example.babyneedscounter

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.statsCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "stats_cache")

/**
 * Cache for storing last fetched baby stats to avoid showing "--:--" during refresh
 */
class StatsCache(private val context: Context) {
    
    companion object {
        private val PEE_COUNT_KEY = intPreferencesKey("cached_pee_count")
        private val POOP_COUNT_KEY = intPreferencesKey("cached_poop_count")
        private val LAST_FEED_TIME_ISO_KEY = stringPreferencesKey("cached_last_feed_time_iso")
        private val CACHE_TIMESTAMP_KEY = longPreferencesKey("cache_timestamp")
        
        // Cache is valid for 5 minutes
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L
    }
    
    /**
     * Save stats to cache
     */
    suspend fun saveStats(stats: BackendService.TodayStats) {
        try {
            context.statsCacheDataStore.edit { preferences ->
                preferences[PEE_COUNT_KEY] = stats.peeCount
                preferences[POOP_COUNT_KEY] = stats.poopCount
                stats.lastFeedTimeISO?.let {
                    preferences[LAST_FEED_TIME_ISO_KEY] = it
                }
                preferences[CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
            }
            Log.d("StatsCache", "Cached stats: pee=${stats.peeCount}, poop=${stats.poopCount}, feed=${stats.lastFeedTimeISO}")
        } catch (e: Exception) {
            Log.e("StatsCache", "Error saving stats to cache", e)
        }
    }
    
    /**
     * Get cached stats if available
     */
    suspend fun getCachedStats(): BackendService.TodayStats? {
        return try {
            val preferences = context.statsCacheDataStore.data.first()
            
            val cacheTimestamp = preferences[CACHE_TIMESTAMP_KEY] ?: 0L
            val peeCount = preferences[PEE_COUNT_KEY]
            val poopCount = preferences[POOP_COUNT_KEY]
            val lastFeedTimeISO = preferences[LAST_FEED_TIME_ISO_KEY]
            
            // Return cached data even if "expired" - we just want to show something
            // The app will refresh in background
            if (peeCount != null && poopCount != null) {
                val age = System.currentTimeMillis() - cacheTimestamp
                Log.d("StatsCache", "Retrieved cached stats (age: ${age}ms): pee=$peeCount, poop=$poopCount")
                BackendService.TodayStats(
                    peeCount = peeCount,
                    poopCount = poopCount,
                    lastFeedTimeISO = lastFeedTimeISO
                )
            } else {
                Log.d("StatsCache", "No cached stats available")
                null
            }
        } catch (e: Exception) {
            Log.e("StatsCache", "Error reading cached stats", e)
            null
        }
    }
    
    /**
     * Check if cache has any data
     */
    suspend fun hasCache(): Boolean {
        return try {
            val preferences = context.statsCacheDataStore.data.first()
            preferences[PEE_COUNT_KEY] != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear the cache
     */
    suspend fun clearCache() {
        try {
            context.statsCacheDataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d("StatsCache", "Cache cleared")
        } catch (e: Exception) {
            Log.e("StatsCache", "Error clearing cache", e)
        }
    }
}
