package com.example.babyneedscounter

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        private val GOOGLE_SHEET_URL_KEY = stringPreferencesKey("google_sheet_url")
    }
    
    val googleSheetUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GOOGLE_SHEET_URL_KEY] ?: ""
        }
    
    suspend fun saveGoogleSheetUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_SHEET_URL_KEY] = url
        }
    }
}
