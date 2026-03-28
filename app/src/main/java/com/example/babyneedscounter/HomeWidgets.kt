package com.example.babyneedscounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class HomeWidgetKind(
    val layoutRes: Int,
) {
    FEED_SMALL(R.layout.widget_feed_small),
    FEED_LARGE(R.layout.widget_feed_large),
    SLEEP_SMALL(R.layout.widget_sleep_small),
    SLEEP_LARGE(R.layout.widget_sleep_large),
}

private class WidgetSnapshotLoader(context: Context) {
    private val appContext = context.applicationContext
    private val settingsManager = SettingsManager(appContext)

    suspend fun loadSnapshot(): WidgetHomeSnapshot {
        val repository = BabyRepository(appContext)
        repository.setSourceUrl(settingsManager.googleSheetUrl.first())
        return repository.widgetSnapshot()
    }

    suspend fun refreshAfterRender(): WidgetHomeSnapshot? {
        val repository = BabyRepository(appContext)
        repository.setSourceUrl(settingsManager.googleSheetUrl.first())
        val result = repository.refresh(RefreshTrigger.Widget, force = false)
        if (!result.success || !result.appliedNetworkUpdate) return null
        return repository.widgetSnapshot()
    }
}

private object WidgetPendingIntents {
    fun openApp(context: Context, requestCode: Int, target: WidgetOpenTarget? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (target != null) {
            WidgetNavigation.applyOpenTarget(intent, target)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun action(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private object HomeWidgetRenderer {
    suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        kind: HomeWidgetKind,
    ) {
        try {
            val snapshotLoader = WidgetSnapshotLoader(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val cachedSnapshot = snapshotLoader.loadSnapshot()
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, kind, cachedSnapshot, options))

            val refreshedSnapshot = snapshotLoader.refreshAfterRender()
            if (refreshedSnapshot != null) {
                appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, kind, refreshedSnapshot, options))
            }
        } catch (e: Exception) {
            Log.e("HomeWidgetRenderer", "Error updating $kind widget", e)
        }
    }

    private fun buildViews(
        context: Context,
        appWidgetId: Int,
        kind: HomeWidgetKind,
        snapshot: WidgetHomeSnapshot,
        options: Bundle,
    ): RemoteViews {
        return RemoteViews(context.packageName, kind.layoutRes).apply {
            when (kind) {
                HomeWidgetKind.FEED_SMALL -> bindFeedSmall(context, appWidgetId, snapshot, options)
                HomeWidgetKind.FEED_LARGE -> bindFeedLarge(context, appWidgetId, snapshot, options)
                HomeWidgetKind.SLEEP_SMALL -> bindSleepSmall(context, appWidgetId, snapshot, options)
                HomeWidgetKind.SLEEP_LARGE -> bindSleepLarge(context, appWidgetId, snapshot, options)
            }
        }
    }

    private fun RemoteViews.bindFeedSmall(
        context: Context,
        appWidgetId: Int,
        snapshot: WidgetHomeSnapshot,
        options: Bundle,
    ) {
        val model = WidgetPresentationMapper.feedSmall(snapshot)
        val typography = HomeWidgetTypography.feedSmall(context, options, model)
        setTextViewText(R.id.widget_feed_small_label, model.label)
        setTextViewText(R.id.widget_feed_small_time, model.primaryText)
        setTextViewTextSize(R.id.widget_feed_small_label, TypedValue.COMPLEX_UNIT_SP, typography.labelSp)
        setTextViewTextSize(R.id.widget_feed_small_time, TypedValue.COMPLEX_UNIT_SP, typography.primarySp)
        setOnClickPendingIntent(
            R.id.widget_feed_small_action,
            WidgetPendingIntents.action(context, WidgetActionReceiver.ACTION_LOG_FEED, appWidgetId * 10 + 1),
        )
        setOnClickPendingIntent(
            R.id.widget_feed_small_content,
            WidgetPendingIntents.openApp(context, appWidgetId * 10 + 2),
        )
    }

    private fun RemoteViews.bindFeedLarge(
        context: Context,
        appWidgetId: Int,
        snapshot: WidgetHomeSnapshot,
        options: Bundle,
    ) {
        val model = WidgetPresentationMapper.feedLarge(snapshot)
        val typography = HomeWidgetTypography.feedLarge(context, options, model)
        setTextViewText(R.id.widget_feed_large_title, model.title)
        setTextViewText(R.id.widget_feed_large_primary, model.primaryText)
        setTextViewText(R.id.widget_feed_large_countdown, model.countdownText)
        setTextViewText(R.id.widget_feed_large_last_feed, model.lastFeedText)
        setTextViewText(R.id.widget_feed_large_count, model.countText)
        setTextViewText(R.id.widget_feed_large_action_label, model.actionLabel)
        setTextViewTextSize(R.id.widget_feed_large_title, TypedValue.COMPLEX_UNIT_SP, typography.titleSp)
        setTextViewTextSize(R.id.widget_feed_large_primary, TypedValue.COMPLEX_UNIT_SP, typography.primarySp)
        setTextViewTextSize(R.id.widget_feed_large_countdown, TypedValue.COMPLEX_UNIT_SP, typography.detailSp)
        setTextViewTextSize(R.id.widget_feed_large_count, TypedValue.COMPLEX_UNIT_SP, typography.detailSp)
        if (typography.footerSp > 0f) {
            setTextViewTextSize(R.id.widget_feed_large_last_feed, TypedValue.COMPLEX_UNIT_SP, typography.footerSp)
        }
        setViewVisibility(
            R.id.widget_feed_large_count,
            if (model.countText.isBlank()) View.GONE else View.VISIBLE,
        )
        setViewVisibility(
            R.id.widget_feed_large_last_feed,
            if (model.lastFeedText.isBlank()) View.GONE else View.VISIBLE,
        )
        setOnClickPendingIntent(
            R.id.widget_feed_large_action,
            WidgetPendingIntents.action(context, WidgetActionReceiver.ACTION_LOG_FEED, appWidgetId * 10 + 2),
        )
        setOnClickPendingIntent(
            R.id.widget_feed_large_content,
            WidgetPendingIntents.openApp(context, appWidgetId * 10 + 3, WidgetOpenTarget.FEED),
        )
    }

    private fun RemoteViews.bindSleepSmall(
        context: Context,
        appWidgetId: Int,
        snapshot: WidgetHomeSnapshot,
        options: Bundle,
    ) {
        val model = WidgetPresentationMapper.sleepSmall(snapshot)
        val typography = HomeWidgetTypography.sleepSmall(context, options, model)
        setImageViewResource(R.id.widget_sleep_small_icon, model.iconRes)
        setTextViewText(R.id.widget_sleep_small_state, model.state)
        setTextViewText(R.id.widget_sleep_small_duration, model.durationText)
        setTextViewTextSize(R.id.widget_sleep_small_state, TypedValue.COMPLEX_UNIT_SP, typography.labelSp)
        setTextViewTextSize(R.id.widget_sleep_small_duration, TypedValue.COMPLEX_UNIT_SP, typography.primarySp)
        setOnClickPendingIntent(
            R.id.widget_sleep_small_action,
            WidgetPendingIntents.action(context, WidgetActionReceiver.ACTION_TOGGLE_SLEEP, appWidgetId * 10 + 4),
        )
        setOnClickPendingIntent(
            R.id.widget_sleep_small_content,
            WidgetPendingIntents.openApp(context, appWidgetId * 10 + 5),
        )
    }

    private fun RemoteViews.bindSleepLarge(
        context: Context,
        appWidgetId: Int,
        snapshot: WidgetHomeSnapshot,
        options: Bundle,
    ) {
        val model = WidgetPresentationMapper.sleepLarge(snapshot)
        val typography = HomeWidgetTypography.sleepLarge(context, options, model)
        setImageViewResource(R.id.widget_sleep_large_action_icon, model.iconRes)
        setTextViewText(R.id.widget_sleep_large_state, model.state)
        setTextViewText(R.id.widget_sleep_large_duration, model.durationText)
        setTextViewText(R.id.widget_sleep_large_previous, model.previousSleepText)
        setTextViewText(R.id.widget_sleep_large_action_label, model.actionLabel)
        setTextViewTextSize(R.id.widget_sleep_large_state, TypedValue.COMPLEX_UNIT_SP, typography.titleSp)
        setTextViewTextSize(R.id.widget_sleep_large_duration, TypedValue.COMPLEX_UNIT_SP, typography.primarySp)
        setTextViewTextSize(R.id.widget_sleep_large_previous, TypedValue.COMPLEX_UNIT_SP, typography.footerSp)
        setOnClickPendingIntent(
            R.id.widget_sleep_large_action,
            WidgetPendingIntents.action(context, WidgetActionReceiver.ACTION_TOGGLE_SLEEP, appWidgetId * 10 + 5),
        )
        setOnClickPendingIntent(
            R.id.widget_sleep_large_content,
            WidgetPendingIntents.openApp(context, appWidgetId * 10 + 6, WidgetOpenTarget.SLEEP),
        )
    }
}

abstract class BaseHomeWidgetProvider protected constructor(
    private val kind: HomeWidgetKind,
) : AppWidgetProvider() {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    HomeWidgetRenderer.update(appContext, appWidgetManager, appWidgetId, kind)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                HomeWidgetRenderer.update(appContext, appWidgetManager, appWidgetId, kind)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class FeedSmallWidget : BaseHomeWidgetProvider(HomeWidgetKind.FEED_SMALL)

class FeedLargeWidget : BaseHomeWidgetProvider(HomeWidgetKind.FEED_LARGE)

class SleepSmallWidget : BaseHomeWidgetProvider(HomeWidgetKind.SLEEP_SMALL)

class SleepLargeWidget : BaseHomeWidgetProvider(HomeWidgetKind.SLEEP_LARGE)
