package com.example.babyneedscounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_LOG_FEED -> {
                HapticFeedback.buttonPress(appContext)
                showToast(appContext, "Logging feed…")
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        logWidgetEvent(
                            context = appContext,
                            pendingLabel = "Feed",
                        ) {
                            BackendService.BabyEvent(
                                timestamp = BackendService.getCurrentTimestamp(),
                                type = ActivityQuickKind.FEED.emojiType(),
                                notes = "",
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TOGGLE_SLEEP -> {
                HapticFeedback.buttonPress(appContext)
                showToast(appContext, "Updating sleep…")
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        logWidgetEvent(
                            context = appContext,
                            pendingLabel = "Sleep",
                        ) { repository ->
                            val snapshot = repository.widgetSnapshot()
                            val nextAction = nextSleepToggleAction(snapshot.stats?.isSleeping() == true)
                            BackendService.BabyEvent(
                                timestamp = BackendService.getCurrentTimestamp(),
                                type = nextAction.eventType,
                                notes = "",
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun logWidgetEvent(
        context: Context,
        pendingLabel: String,
        eventFactory: suspend (BabyRepository) -> BackendService.BabyEvent,
    ) {
        val appContext = context.applicationContext
        try {
            val repository = BabyRepository(appContext)
            repository.setSourceUrl(SettingsManager(appContext).googleSheetUrl.first())

            val event = eventFactory(repository)
            val result = repository.logEvent(event)
            Handler(Looper.getMainLooper()).post {
                if (result.success) {
                    HapticFeedback.success(appContext)
                    showToast(appContext, "$pendingLabel saved")
                    WidgetUpdater.requestUpdateAll(appContext)
                } else {
                    HapticFeedback.error(appContext)
                    showToast(appContext, "Couldn't save $pendingLabel")
                }
            }
        } catch (_: Exception) {
            Handler(Looper.getMainLooper()).post {
                HapticFeedback.error(appContext)
                showToast(appContext, "Couldn't save $pendingLabel")
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_LOG_FEED = "com.example.babyneedscounter.action.WIDGET_LOG_FEED"
        const val ACTION_TOGGLE_SLEEP = "com.example.babyneedscounter.action.WIDGET_TOGGLE_SLEEP"
    }
}
