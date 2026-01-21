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

class BabyFeedTimesWidget : AppWidgetProvider() {

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
        Log.d("BabyFeedTimesWidget", "Feed times widget enabled")
    }

    override fun onDisabled(context: Context) {
        Log.d("BabyFeedTimesWidget", "Feed times widget disabled")
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_feed_times)

                // Set up tap to open app
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_feed_times_root, pendingIntent)

                // Fetch and display feed times asynchronously
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        val settingsManager = SettingsManager(context)
                        val googleSheetUrl = settingsManager.googleSheetUrl.first()
                        
                        if (googleSheetUrl.isNotEmpty()) {
                            val backendService = BackendService(context)
                            
                            // 1. First, load cached data immediately for instant display
                            val cachedStats = backendService.getCachedStats()
                            if (cachedStats != null) {
                                Log.d("BabyFeedTimesWidget", "Displaying cached feed times immediately")
                                Handler(Looper.getMainLooper()).post {
                                    views.setTextViewText(R.id.widget_feed_previous_time, cachedStats.getTimeUntilNextFeed())
                                    views.setTextViewText(R.id.widget_feed_next_time, cachedStats.getNextFeedTime())
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            } else {
                                // Set default values if no cache
                                Handler(Looper.getMainLooper()).post {
                                    views.setTextViewText(R.id.widget_feed_previous_time, "—")
                                    views.setTextViewText(R.id.widget_feed_next_time, "—")
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            }
                            
                            // 2. Then fetch fresh data in background
                            val stats = backendService.fetchTodayStats(googleSheetUrl, useCache = true)
                            
                            if (stats != null) {
                                Log.d("BabyFeedTimesWidget", "Updating with fresh feed times")
                                Handler(Looper.getMainLooper()).post {
                                    views.setTextViewText(R.id.widget_feed_previous_time, stats.getTimeUntilNextFeed())
                                    views.setTextViewText(R.id.widget_feed_next_time, stats.getNextFeedTime())
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                views.setTextViewText(R.id.widget_feed_previous_time, "—")
                                views.setTextViewText(R.id.widget_feed_next_time, "—")
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BabyFeedTimesWidget", "Error fetching feed times", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("BabyFeedTimesWidget", "Error updating widget", e)
            }
        }
    }
}
