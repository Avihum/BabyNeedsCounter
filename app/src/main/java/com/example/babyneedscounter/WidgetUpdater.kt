package com.example.babyneedscounter

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

object WidgetUpdater {
    fun requestUpdateAll(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            requestUpdate(context, appWidgetManager, FeedSmallWidget::class.java)
            requestUpdate(context, appWidgetManager, FeedLargeWidget::class.java)
            requestUpdate(context, appWidgetManager, SleepSmallWidget::class.java)
            requestUpdate(context, appWidgetManager, SleepLargeWidget::class.java)
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Error requesting widget updates", e)
        }
    }

    private fun requestUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        receiverClass: Class<*>,
    ) {
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, receiverClass))
        if (widgetIds.isEmpty()) return

        val intent = Intent(context, receiverClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(intent)
    }
}
