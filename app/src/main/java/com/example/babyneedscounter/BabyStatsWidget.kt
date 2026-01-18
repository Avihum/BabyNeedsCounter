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

class BabyStatsWidget : AppWidgetProvider() {

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
        Log.d("BabyStatsWidget", "Stats widget enabled")
    }

    override fun onDisabled(context: Context) {
        Log.d("BabyStatsWidget", "Stats widget disabled")
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_baby_stats)

                // Set up tap to open app
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_stats_root, pendingIntent)

                // Fetch and display today's stats
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        val settingsManager = SettingsManager(context)
                        val googleSheetUrl = settingsManager.googleSheetUrl.first()
                        
                        if (googleSheetUrl.isNotEmpty()) {
                            val backendService = BackendService(context)
                            val stats = backendService.fetchTodayStats(googleSheetUrl)
                            
                            if (stats != null) {
                                Handler(Looper.getMainLooper()).post {
                                    views.setTextViewText(R.id.widget_stats_pee_count, stats.peeCount.toString())
                                    views.setTextViewText(R.id.widget_stats_poop_count, stats.poopCount.toString())
                                    views.setTextViewText(R.id.widget_stats_feed_time, stats.getTimeSinceLastFeed())
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                views.setTextViewText(R.id.widget_stats_pee_count, "—")
                                views.setTextViewText(R.id.widget_stats_poop_count, "—")
                                views.setTextViewText(R.id.widget_stats_feed_time, "—")
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BabyStatsWidget", "Error fetching stats", e)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("BabyStatsWidget", "Error updating widget", e)
            }
        }
    }
}
