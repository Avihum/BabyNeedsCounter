package com.example.babyneedscounter

import java.time.ZoneId
import kotlin.math.abs

/**
 * Aggregations for Insights: **7am–7am baby days**, sleep/wake from 😴/☀️ pairs, feeds/diapers by event timestamp.
 */
object StatsAggregation {

    data class SleepDayStatsPlaceholder(
        val totalSleepMinutes: Int?,
        val longestSleepMinutes: Int?,
        val wakeWindowSummaries: List<String>
    )

    data class FeedingDayStatsPlaceholder(
        val feedCount: Int,
        val intervalMinutesBetweenFeeds: List<Long>
    )

    data class DiaperDayStatsPlaceholder(
        val diaperCount: Int,
        val peeOnlyCount: Int,
        val poopRelatedCount: Int
    )

    /** Single day bundle for Insights cards (same layout for Today / Yesterday). */
    data class InsightDayBundle(
        val sleep: SleepDayStatsPlaceholder,
        val feed: FeedingDayStatsPlaceholder,
        val diaper: DiaperDayStatsPlaceholder
    )

    fun insightBundleForBabyDay(
        allEventsChronological: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        capFeedDiaperLogAtNow: Boolean
    ): InsightDayBundle {
        val now = System.currentTimeMillis()
        val dayEvents = eventsForBabyDay(allEventsChronological, babyDayStartMs, zone, capFeedDiaperLogAtNow, now)
        return InsightDayBundle(
            sleep = sleepStatsForBabyDay(allEventsChronological, babyDayStartMs, zone),
            feed = feedingStatsFromEvents(dayEvents),
            diaper = diaperStatsFromEvents(dayEvents)
        )
    }

    fun feedingStatsFromEvents(events: List<BackendService.EventItem>): FeedingDayStatsPlaceholder {
        val feeds = events.filter { it.type.contains("🐄") }
            .mapNotNull { parseEventTimeMs(it.timestamp) }
            .sorted()
        val intervals = mutableListOf<Long>()
        for (i in 1 until feeds.size) {
            intervals.add((feeds[i] - feeds[i - 1]) / 60_000L)
        }
        return FeedingDayStatsPlaceholder(
            feedCount = feeds.size,
            intervalMinutesBetweenFeeds = intervals
        )
    }

    fun diaperStatsFromEvents(events: List<BackendService.EventItem>): DiaperDayStatsPlaceholder {
        val diaperEvents = events.filter { it.type.contains("💧") || it.type.contains("💩") }
        return DiaperDayStatsPlaceholder(
            diaperCount = diaperEvents.size,
            peeOnlyCount = events.count { it.type == "💧" },
            poopRelatedCount = events.count { it.type.contains("💩") }
        )
    }

    /**
     * Events in the baby day **[babyDayStartMs, next 7am)**.
     * If [capAtNow] and this interval is the **current** baby day, drops events after [nowMs].
     */
    fun eventsForBabyDay(
        events: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId,
        capAtNow: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): List<BackendService.EventItem> {
        val endExclusive = BabyDay.nextBabyDayStartMs(babyDayStartMs, zone)
        val currentBabyDayStart = BabyDay.babyDayStartContaining(nowMs, zone)
        return events.mapNotNull { e ->
            val t = parseEventTimeMs(e.timestamp) ?: return@mapNotNull null
            if (t < babyDayStartMs || t >= endExclusive) return@mapNotNull null
            if (capAtNow && babyDayStartMs == currentBabyDayStart && t > nowMs) return@mapNotNull null
            e
        }.sortedByDescending { parseEventTimeMs(it.timestamp) ?: 0L }
    }

    /** Formats average interval: minutes only if under 60, else "Xh Ym" / "Xh". */
    fun formatAverageFeedIntervalMinutes(avgMinutes: Int): String {
        if (avgMinutes < 60) return "${avgMinutes}m"
        val h = avgMinutes / 60
        val m = avgMinutes % 60
        return if (m > 0) "${h}h ${m}m" else "${h}h"
    }

    /**
     * Sleep totals and wake windows for the baby day starting at [babyDayStartMs]:
     * sleep credited when 😴 falls in **[start, next 7am)**; wake window when ☀️ falls in that range.
     */
    fun sleepStatsForBabyDay(
        allEventsSortedOrNot: List<BackendService.EventItem>,
        babyDayStartMs: Long,
        zone: ZoneId
    ): SleepDayStatsPlaceholder {
        val endExclusive = BabyDay.nextBabyDayStartMs(babyDayStartMs, zone)
        val sorted = allEventsSortedOrNot.sortedBy { parseEventTimeMs(it.timestamp) ?: 0L }
        var sleepStartMs: Long? = null
        var wakeStartMs: Long? = null
        var totalMsForDay = 0L
        var longestMsForDay = 0L
        val wakeDurationsForDay = mutableListOf<Long>()

        fun inDay(t: Long) = t >= babyDayStartMs && t < endExclusive

        for (event in sorted) {
            val t = parseEventTimeMs(event.timestamp) ?: continue
            when {
                isSleepStart(event.type) -> {
                    if (wakeStartMs != null) {
                        val ww = t - wakeStartMs
                        if (ww > 0 && inDay(wakeStartMs)) {
                            wakeDurationsForDay.add(ww)
                        }
                    }
                    wakeStartMs = null
                    sleepStartMs = t
                }
                isSleepEnd(event.type) -> {
                    if (sleepStartMs != null) {
                        val duration = t - sleepStartMs
                        if (duration > 0 && inDay(sleepStartMs)) {
                            totalMsForDay += duration
                            if (duration > longestMsForDay) longestMsForDay = duration
                        }
                        sleepStartMs = null
                    }
                    wakeStartMs = t
                }
            }
        }

        val totalMin = if (totalMsForDay > 0) (totalMsForDay / 60_000L).toInt() else null
        val longestMin = if (longestMsForDay > 0) (longestMsForDay / 60_000L).toInt() else null
        val wakeSummaries = wakeDurationsForDay
            .map { formatShortDurationMinutes((it / 60_000L).toInt()) }
            .filter { it.isNotEmpty() }

        return SleepDayStatsPlaceholder(
            totalSleepMinutes = totalMin,
            longestSleepMinutes = longestMin,
            wakeWindowSummaries = wakeSummaries
        )
    }

    fun formatShortDurationMinutes(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /** Signed delta for sleep: anchor − compare (e.g. current baby day − previous). */
    fun formatSleepDeltaMinutes(anchorMinutes: Int?, compareMinutes: Int?): String {
        val a = anchorMinutes ?: 0
        val b = compareMinutes ?: 0
        val delta = a - b
        if (delta == 0) return "0m"
        val sign = if (delta > 0) "+" else "-"
        val body = formatShortDurationMinutes(abs(delta))
        return "$sign$body"
    }

    fun formatCountDelta(anchor: Int, compare: Int): String {
        val delta = anchor - compare
        return when {
            delta > 0 -> "+$delta"
            delta < 0 -> "$delta"
            else -> "0"
        }
    }

    private fun isSleepStart(type: String): Boolean =
        type == SheetEventMarkers.SLEEP_STARTED || type == "sleep_started"

    private fun isSleepEnd(type: String): Boolean =
        type == SheetEventMarkers.SLEEP_ENDED || type == "sleep_ended"

    fun parseEventTimeMs(iso: String): Long? {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            fmt.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }
}
