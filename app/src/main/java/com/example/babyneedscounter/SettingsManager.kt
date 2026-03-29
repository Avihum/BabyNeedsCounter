package com.example.babyneedscounter

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        private val GOOGLE_SHEET_URL_KEY = stringPreferencesKey("google_sheet_url")
        private val GOOGLE_SHEET_VIEW_URL_KEY = stringPreferencesKey("google_sheet_view_url")
        private val WEB_APP_URL_VERIFIED_KEY = booleanPreferencesKey("web_app_url_verified")
        private val VIEW_URL_VERIFIED_KEY = booleanPreferencesKey("view_url_verified")
        
        private const val DEFAULT_SHEET_ID = "1FnAb9F0Iu3A0DnO4tl1W3NoM0ck5d51buzp72VNIEq4"

        // Default URLs
        private const val DEFAULT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycby2KtzVy9fvED4PPdsxyYGdCDlhDvgmPB3fKsdraYbgvO3EwI3WdQNEoSZ9Grnld73U/exec"
        private const val DEFAULT_VIEW_URL = "https://docs.google.com/spreadsheets/d/$DEFAULT_SHEET_ID/edit?gid=0#gid=0"
    }
    
    val googleSheetUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GOOGLE_SHEET_URL_KEY]?.takeIf { it.isNotEmpty() } ?: DEFAULT_WEB_APP_URL
        }
    
    val googleSheetViewUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GOOGLE_SHEET_VIEW_URL_KEY]?.takeIf { it.isNotEmpty() } ?: DEFAULT_VIEW_URL
        }
    
    val webAppUrlVerified: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[WEB_APP_URL_VERIFIED_KEY] ?: false
        }
    
    val viewUrlVerified: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIEW_URL_VERIFIED_KEY] ?: false
        }
    
    suspend fun saveGoogleSheetUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_SHEET_URL_KEY] = url
        }
    }
    
    suspend fun saveGoogleSheetViewUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_SHEET_VIEW_URL_KEY] = url
        }
    }
    
    suspend fun saveWebAppUrlVerified(verified: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WEB_APP_URL_VERIFIED_KEY] = verified
        }
    }
    
    suspend fun saveViewUrlVerified(verified: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_URL_VERIFIED_KEY] = verified
        }
    }
}
