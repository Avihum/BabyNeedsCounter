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
import android.widget.Toast
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
                // Strong haptic feedback with satisfying click pattern
                HapticFeedback.buttonPress(context)
                Log.d("BabyNeeds", "Widget: Logged Poop & Pee")
                Pair("💩💧", "💩💧 Poop & Pee")
            }
            ACTION_PEE -> {
                // Strong haptic feedback with satisfying click pattern
                HapticFeedback.buttonPress(context)
                Log.d("BabyNeeds", "Widget: Logged Pee Only")
                Pair("💧", "💧 Pee")
            }
            ACTION_FEED -> {
                // Strong haptic feedback with satisfying click pattern
                HapticFeedback.buttonPress(context)
                Log.d("BabyNeeds", "Widget: Logged Feed (Breastmilk)")
                Pair("🐄", "🐄 Feed")
            }
            ACTION_PEE_FEED -> {
                // Strong haptic feedback with satisfying click pattern
                HapticFeedback.buttonPress(context)
                Log.d("BabyNeeds", "Widget: Logged Pee + Feed")
                Pair("💧🐄", "💧🐄 Pee + Feed")
            }
            ACTION_POOP_FEED -> {
                // Strong haptic feedback with satisfying click pattern
                HapticFeedback.buttonPress(context)
                Log.d("BabyNeeds", "Widget: Logged Poop + Feed")
                Pair("💩🐄", "💩🐄 Poop + Feed")
            }
            ACTION_OPEN_APP -> {
                HapticFeedback.lightTap(context)
                Log.d("BabyNeeds", "Widget: Opening app")
                val appIntent = Intent(context, MainActivity::class.java)
                appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(appIntent)
                null
            }
            else -> null
        }
        
        eventInfo?.let { (eventType, displayName) ->
            // Show immediate visual feedback
            Toast.makeText(context, "📝 Logging $displayName...", Toast.LENGTH_SHORT).show()
            syncToBackend(context, eventType, displayName)
        }
    }
    
    private fun syncToBackend(context: Context, eventType: String, displayName: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val repository = BabyRepository(context)
                repository.setSourceUrl(SettingsManager(context).googleSheetUrl.first())
                val success = repository.logEvent(
                    BackendService.BabyEvent(
                        timestamp = BackendService.getCurrentTimestamp(),
                        type = eventType,
                        notes = ""
                    )
                ).success

                Handler(Looper.getMainLooper()).post {
                    if (success) {
                        HapticFeedback.success(context)
                        Toast.makeText(context, "✓ $displayName tracked!", Toast.LENGTH_SHORT).show()
                        Log.d("BabyNeeds", "Widget: Successfully synced - $displayName")
                        updateStatsWidgets(context)
                    } else {
                        HapticFeedback.error(context)
                        Toast.makeText(context, "❌ Failed to save", Toast.LENGTH_SHORT).show()
                        Log.e("BabyNeeds", "Widget: Failed to sync")
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    HapticFeedback.error(context)
                }
                Log.e("BabyNeeds", "Widget: Error syncing to backend", e)
            }
        }
    }

    private fun updateStatsWidgets(context: Context) {
        WidgetUpdater.requestUpdateAll(context)
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
