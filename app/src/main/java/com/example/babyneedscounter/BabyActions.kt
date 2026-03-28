package com.example.babyneedscounter

import android.content.Intent
import java.util.Locale

enum class ActivityQuickKind {
    POOP_PEE,
    PEE,
    FEED,
}

fun ActivityQuickKind.emojiType(): String = when (this) {
    ActivityQuickKind.POOP_PEE -> "💩💧"
    ActivityQuickKind.PEE -> "💧"
    ActivityQuickKind.FEED -> "🐄"
}

fun ActivityQuickKind.displayName(): String = when (this) {
    ActivityQuickKind.POOP_PEE -> "Poop & Pee"
    ActivityQuickKind.PEE -> "Pee"
    ActivityQuickKind.FEED -> "Feed"
}

data class SleepToggleAction(
    val eventType: String,
    val label: String,
)

fun nextSleepToggleAction(isSleeping: Boolean): SleepToggleAction =
    if (isSleeping) {
        SleepToggleAction(
            eventType = SheetEventMarkers.SLEEP_ENDED,
            label = "wake-up",
        )
    } else {
        SleepToggleAction(
            eventType = SheetEventMarkers.SLEEP_STARTED,
            label = "sleep start",
        )
    }

enum class WidgetOpenTarget {
    FEED,
    SLEEP;

    companion object {
        fun fromRaw(raw: String?): WidgetOpenTarget? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == raw.trim().uppercase(Locale.US) }
        }
    }
}

object WidgetNavigation {
    const val EXTRA_OPEN_TARGET = "com.example.babyneedscounter.extra.WIDGET_OPEN_TARGET"

    fun applyOpenTarget(intent: Intent, target: WidgetOpenTarget): Intent {
        intent.putExtra(EXTRA_OPEN_TARGET, target.name)
        return intent
    }

    fun extractOpenTarget(intent: Intent?): WidgetOpenTarget? {
        return WidgetOpenTarget.fromRaw(intent?.getStringExtra(EXTRA_OPEN_TARGET))
    }
}
