package com.example.babyneedscounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

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
        
        when (intent.action) {
            ACTION_POOP_PEE -> {
                Log.d("BabyNeeds", "Widget: Logged Poop & Pee")
                // TODO: Save to database when data layer is implemented
            }
            ACTION_PEE -> {
                Log.d("BabyNeeds", "Widget: Logged Pee Only")
                // TODO: Save to database when data layer is implemented
            }
            ACTION_FEED -> {
                Log.d("BabyNeeds", "Widget: Logged Feed (Breastmilk)")
                // TODO: Save to database when data layer is implemented
            }
        }
    }

    companion object {
        private const val ACTION_POOP_PEE = "com.example.babyneedscounter.ACTION_POOP_PEE"
        private const val ACTION_PEE = "com.example.babyneedscounter.ACTION_PEE"
        private const val ACTION_FEED = "com.example.babyneedscounter.ACTION_FEED"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
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

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
