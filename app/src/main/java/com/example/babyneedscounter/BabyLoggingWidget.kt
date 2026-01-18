package com.example.babyneedscounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BabyLoggingWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d("BabyLoggingWidget", "Logging widget enabled")
    }

    override fun onDisabled(context: Context) {
        Log.d("BabyLoggingWidget", "Logging widget disabled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val eventInfo = when (intent.action) {
            ACTION_POOP_PEE -> {
                Log.d("BabyNeeds", "Widget: Logged Poop & Pee")
                Pair("💩💧", "💩💧 Poop & Pee")
            }
            ACTION_PEE -> {
                Log.d("BabyNeeds", "Widget: Logged Pee Only")
                Pair("💧", "💧 Pee")
            }
            ACTION_FEED -> {
                Log.d("BabyNeeds", "Widget: Logged Feed (Breastmilk)")
                Pair("🐄", "🐄 Feed")
            }
            ACTION_PEE_FEED -> {
                Log.d("BabyNeeds", "Widget: Logged Pee + Feed")
                Pair("💧🐄", "💧🐄 Pee + Feed")
            }
            ACTION_POOP_FEED -> {
                Log.d("BabyNeeds", "Widget: Logged Poop + Feed")
                Pair("💩🐄", "💩🐄 Poop + Feed")
            }
            ACTION_OPEN_APP -> {
                Log.d("BabyNeeds", "Widget: Opening app")
                val appIntent = Intent(context, MainActivity::class.java)
                appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(appIntent)
                null
            }
            else -> null
        }
        
        eventInfo?.let { (eventType, displayName) ->
            syncToBackend(context, eventType, displayName)
        }
    }
    
    private fun syncToBackend(context: Context, eventType: String, displayName: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settingsManager = SettingsManager(context)
                val googleSheetUrl = settingsManager.googleSheetUrl.first()
                
                if (googleSheetUrl.isNotEmpty()) {
                    val backendService = BackendService(context)
                    val event = BackendService.BabyEvent(
                        timestamp = BackendService.getCurrentTimestamp(),
                        type = eventType,
                        notes = ""
                    )
                    val success = backendService.logEvent(googleSheetUrl, event)
                    
                    Handler(Looper.getMainLooper()).post {
                        if (success) {
                            Log.d("BabyNeeds", "Widget: Successfully synced - $displayName")
                            // Update stats widgets
                            updateStatsWidgets(context)
                        } else {
                            Log.e("BabyNeeds", "Widget: Failed to sync")
                        }
                    }
                } else {
                    Log.w("BabyNeeds", "Widget: No Google Sheet URL configured")
                }
            } catch (e: Exception) {
                Log.e("BabyNeeds", "Widget: Error syncing to backend", e)
            }
        }
    }

    private fun updateStatsWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val statsWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, BabyStatsWidget::class.java)
        )
        for (widgetId in statsWidgetIds) {
            BabyStatsWidget.updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        private const val ACTION_POOP_PEE = "com.example.babyneedscounter.ACTION_POOP_PEE"
        private const val ACTION_PEE = "com.example.babyneedscounter.ACTION_PEE"
        private const val ACTION_FEED = "com.example.babyneedscounter.ACTION_FEED"
        private const val ACTION_PEE_FEED = "com.example.babyneedscounter.ACTION_PEE_FEED"
        private const val ACTION_POOP_FEED = "com.example.babyneedscounter.ACTION_POOP_FEED"
        private const val ACTION_OPEN_APP = "com.example.babyneedscounter.ACTION_OPEN_APP"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_baby_logging)

                // Set up button click intents
                views.setOnClickPendingIntent(
                    R.id.widget_logging_btn_poop_pee,
                    getPendingSelfIntent(context, ACTION_POOP_PEE)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_logging_btn_pee,
                    getPendingSelfIntent(context, ACTION_PEE)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_logging_btn_feed,
                    getPendingSelfIntent(context, ACTION_FEED)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_logging_btn_pee_feed,
                    getPendingSelfIntent(context, ACTION_PEE_FEED)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_logging_btn_poop_feed,
                    getPendingSelfIntent(context, ACTION_POOP_FEED)
                )
                
                // Set up tap on root to open app
                views.setOnClickPendingIntent(
                    R.id.widget_logging_root,
                    getPendingSelfIntent(context, ACTION_OPEN_APP)
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("BabyLoggingWidget", "Error updating widget", e)
            }
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, BabyLoggingWidget::class.java)
            intent.action = action
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
