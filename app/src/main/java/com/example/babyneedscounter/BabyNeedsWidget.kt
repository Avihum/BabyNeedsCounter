package com.example.babyneedscounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BabyNeedsWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d("BabyNeedsWidget", "Widget enabled")
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d("BabyNeedsWidget", "Widget disabled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val eventInfo = when (intent.action) {
            ACTION_POOP_PEE -> {
                // Haptic feedback on button press
                HapticFeedback.mediumImpact(context)
                Log.d("BabyNeeds", "Widget: Logged Poop & Pee")
                Pair("💩💧", "💩 Poop & Pee")
            }
            ACTION_PEE -> {
                // Haptic feedback on button press
                HapticFeedback.mediumImpact(context)
                Log.d("BabyNeeds", "Widget: Logged Pee Only")
                Pair("💧", "💧 Pee")
            }
            ACTION_FEED -> {
                // Haptic feedback on button press
                HapticFeedback.mediumImpact(context)
                Log.d("BabyNeeds", "Widget: Logged Feed (Breastmilk)")
                Pair("🐄", "🐄 Feed")
            }
            ACTION_PEE_FEED -> {
                // Haptic feedback on button press
                HapticFeedback.mediumImpact(context)
                Log.d("BabyNeeds", "Widget: Logged Pee + Feed")
                Pair("💧🐄", "💧🐄 Pee + Feed")
            }
            ACTION_POOP_FEED -> {
                // Haptic feedback on button press
                HapticFeedback.mediumImpact(context)
                Log.d("BabyNeeds", "Widget: Logged Poop + Feed")
                Pair("💩🐄", "💩🐄 Poop + Feed")
            }
            else -> null
        }
        
        // Sync to backend if an event was triggered
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
                        Log.d("BabyNeeds", "Widget: Successfully synced to Google Sheets - $displayName")
                        WidgetUpdater.requestUpdateAll(context)
                    } else {
                        HapticFeedback.error(context)
                        Toast.makeText(context, "❌ Failed to save", Toast.LENGTH_SHORT).show()
                        Log.e("BabyNeeds", "Widget: Failed to sync to Google Sheets")
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Handler(Looper.getMainLooper()).post {
                        HapticFeedback.error(context)
                    }
                }
                Log.e("BabyNeeds", "Widget: Error syncing to backend", e)
            }
        }
    }

    companion object {
        private const val ACTION_POOP_PEE = "com.example.babyneedscounter.ACTION_POOP_PEE"
        private const val ACTION_PEE = "com.example.babyneedscounter.ACTION_PEE"
        private const val ACTION_FEED = "com.example.babyneedscounter.ACTION_FEED"
        private const val ACTION_PEE_FEED = "com.example.babyneedscounter.ACTION_PEE_FEED"
        private const val ACTION_POOP_FEED = "com.example.babyneedscounter.ACTION_POOP_FEED"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                // Construct the RemoteViews object
                val views = RemoteViews(context.packageName, R.layout.widget_baby_needs)

                // Set up button click intents
                views.setOnClickPendingIntent(
                    R.id.widget_btn_poop_pee,
                    getPendingSelfIntent(context, ACTION_POOP_PEE)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_pee,
                    getPendingSelfIntent(context, ACTION_PEE)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_feed,
                    getPendingSelfIntent(context, ACTION_FEED)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_pee_feed,
                    getPendingSelfIntent(context, ACTION_PEE_FEED)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_poop_feed,
                    getPendingSelfIntent(context, ACTION_POOP_FEED)
                )

                // Fetch and display today's stats
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        val settingsManager = SettingsManager(context)
                        val googleSheetUrl = settingsManager.googleSheetUrl.first()
                        
                        if (googleSheetUrl.isNotEmpty()) {
                            val repository = BabyRepository(context)
                            repository.setSourceUrl(googleSheetUrl)

                            var stats = repository.cachedHomeStats()
                            if (stats == null) {
                                repository.refresh(RefreshTrigger.Manual, force = true)
                                stats = repository.cachedHomeStats()
                            }

                            views.setTextViewText(R.id.widget_pee_count, stats?.peeCount?.toString() ?: "—")
                            views.setTextViewText(R.id.widget_poop_count, stats?.poopCount?.toString() ?: "—")
                            views.setTextViewText(R.id.widget_feed_time, stats?.getTimeSinceLastFeed() ?: "—")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } catch (e: Exception) {
                        Log.e("BabyNeedsWidget", "Error fetching stats", e)
                    }
                }

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("BabyNeedsWidget", "Error updating widget", e)
            }
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, BabyNeedsWidget::class.java)
            intent.action = action
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
