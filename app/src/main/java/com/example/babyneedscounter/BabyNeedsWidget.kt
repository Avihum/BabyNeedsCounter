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
                Log.d("BabyNeeds", "Widget: Logged Poop & Pee")
                Pair("💩💧", "💩 Poop & Pee")
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
            else -> null
        }
        
        // Show feedback and sync to backend if an event was triggered
        eventInfo?.let { (eventType, displayName) ->
            showFeedback(context, displayName, true)
            syncToBackend(context, eventType, displayName)
        }
    }
    
    private fun showFeedback(context: Context, message: String, isLoading: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, BabyNeedsWidget::class.java)
        )
        
        for (widgetId in widgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_baby_needs)
            
            if (isLoading) {
                views.setTextViewText(R.id.widget_status_text, "Saving...")
                views.setViewVisibility(R.id.widget_last_event, View.GONE)
            } else {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                views.setTextViewText(R.id.widget_status_text, message)
                views.setTextViewText(R.id.widget_last_event, "✓ ${timeFormat.format(Date())}")
                views.setViewVisibility(R.id.widget_last_event, View.VISIBLE)
            }
            
            // Re-attach click listeners
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
            
            appWidgetManager.updateAppWidget(widgetId, views)
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
                    
                    // Update UI with result on main thread
                    Handler(Looper.getMainLooper()).post {
                        if (success) {
                            Log.d("BabyNeeds", "Widget: Successfully synced to Google Sheets")
                            showFeedback(context, "$displayName tracked", false)
                        } else {
                            Log.e("BabyNeeds", "Widget: Failed to sync to Google Sheets")
                            showFeedback(context, "Couldn't save", false)
                        }
                        
                        // Reset to default after 3 seconds
                        Handler(Looper.getMainLooper()).postDelayed({
                            showFeedback(context, "Ready to track", false)
                        }, 3000)
                    }
                } else {
                    Log.w("BabyNeeds", "Widget: No Google Sheet URL configured")
                    Handler(Looper.getMainLooper()).post {
                        showFeedback(context, "Open app to set up", false)
                    }
                }
            } catch (e: Exception) {
                Log.e("BabyNeeds", "Widget: Error syncing to backend", e)
                Handler(Looper.getMainLooper()).post {
                    showFeedback(context, "Couldn't save", false)
                }
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
