package com.example.babyneedscounter

fun sleepDurationPrimaryLine(
    todayStats: BackendService.TodayStats?,
    sleeping: Boolean,
    awakeFromLastWake: Boolean,
): String {
    if (todayStats == null) return "—"
    return when {
        sleeping -> todayStats.getSleepDurationLabel()
        awakeFromLastWake -> todayStats.getAwakeDurationLabel()
        else -> "—"
    }
}

fun formatPreviousSleepLine(previousSleepDurationLabel: String?): String {
    val label = previousSleepDurationLabel?.takeIf { it.isNotBlank() } ?: "—"
    return "Previous sleep cycle: $label"
}
