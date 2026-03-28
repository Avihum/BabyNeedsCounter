package com.example.babyneedscounter

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPresentationTest {
    @Test
    fun feedSmall_usesNextFeedTimeWhenFeedDataExists() {
        val stats = feedStats(lastFeedOffsetMinutes = 60, feedCount = 3)
        val snapshot = widgetSnapshot(stats = stats, hasCachedContent = true)

        val model = WidgetPresentationMapper.feedSmall(snapshot)

        assertEquals("Next feed", model.label)
        assertEquals(stats.getNextFeedTime(), model.primaryText)
    }

    @Test
    fun feedLarge_showsDueNowWhenFeedIsOverdue() {
        val snapshot = widgetSnapshot(
            stats = feedStats(lastFeedOffsetMinutes = 240, feedCount = 5),
            hasCachedContent = true,
        )

        val model = WidgetPresentationMapper.feedLarge(snapshot)

        assertEquals("Due now", model.countdownText)
        assertEquals("5 today", model.countText)
    }

    @Test
    fun widgets_useMeaningfulCachedFallbacksWhenDataIsIncomplete() {
        val cachedSnapshot = widgetSnapshot(
            stats = BackendService.TodayStats(
                peeCount = 0,
                poopCount = 0,
                feedCount = 0,
                lastFeedTimeISO = null,
            ),
            hasCachedContent = true,
        )

        val feedLarge = WidgetPresentationMapper.feedLarge(cachedSnapshot)
        val sleepSmall = WidgetPresentationMapper.sleepSmall(cachedSnapshot)

        assertEquals("No feed yet", feedLarge.primaryText)
        assertEquals("Log the first feed in app", feedLarge.countdownText)
        assertEquals("No sleep yet", sleepSmall.durationText)
    }

    @Test
    fun sleepWidgets_mapSleepingState() {
        val snapshot = widgetSnapshot(
            stats = sleepStats(
                eventType = SheetEventMarkers.SLEEP_STARTED,
                eventOffsetMinutes = 40,
                previousSleepDurationLabel = "2h 10m",
            ),
            hasCachedContent = true,
        )

        val small = WidgetPresentationMapper.sleepSmall(snapshot)
        val large = WidgetPresentationMapper.sleepLarge(snapshot)

        assertEquals("Sleeping", small.state)
        assertEquals(R.drawable.marshmallow_sleep, small.iconRes)
        assertEquals("Sleeping", large.state)
        assertEquals("Stop", large.actionLabel)
        assertEquals("Previous sleep cycle: 2h 10m", large.previousSleepText)
        assertTrue(large.durationText.contains("m"))
    }

    @Test
    fun sleepWidgets_mapAwakeState() {
        val snapshot = widgetSnapshot(
            stats = sleepStats(
                eventType = SheetEventMarkers.SLEEP_ENDED,
                eventOffsetMinutes = 55,
                previousSleepDurationLabel = "1h 45m",
            ),
            hasCachedContent = true,
        )

        val small = WidgetPresentationMapper.sleepSmall(snapshot)
        val large = WidgetPresentationMapper.sleepLarge(snapshot)

        assertEquals("Awake", small.state)
        assertEquals(R.drawable.marshmallow_awake, small.iconRes)
        assertEquals("Awake", large.state)
        assertEquals("Start", large.actionLabel)
        assertEquals("Previous sleep cycle: 1h 45m", large.previousSleepText)
    }

    @Test
    fun widgets_showMeaningfulFallbacksWhenNoCachedDataExists() {
        val snapshot = widgetSnapshot(stats = null, hasCachedContent = false)

        val feedSmall = WidgetPresentationMapper.feedSmall(snapshot)
        val sleepLarge = WidgetPresentationMapper.sleepLarge(snapshot)

        assertEquals("Open app", feedSmall.primaryText)
        assertEquals("Open app to sync", sleepLarge.durationText)
        assertEquals("Last saved sleep data will appear here", sleepLarge.previousSleepText)
    }

    @Test
    fun nextSleepToggleAction_matchesAppBehavior() {
        val sleepingAction = nextSleepToggleAction(isSleeping = true)
        val awakeAction = nextSleepToggleAction(isSleeping = false)

        assertEquals(SheetEventMarkers.SLEEP_ENDED, sleepingAction.eventType)
        assertEquals("wake-up", sleepingAction.label)
        assertEquals(SheetEventMarkers.SLEEP_STARTED, awakeAction.eventType)
        assertEquals("sleep start", awakeAction.label)
    }

    private fun widgetSnapshot(
        stats: BackendService.TodayStats?,
        hasCachedContent: Boolean,
    ): WidgetHomeSnapshot {
        return WidgetHomeSnapshot(
            sourceUrl = "https://example.test",
            stats = stats,
            hasCachedContent = hasCachedContent,
            lastAttemptAt = null,
            lastSuccessAt = System.currentTimeMillis(),
        )
    }

    private fun feedStats(
        lastFeedOffsetMinutes: Long,
        feedCount: Int,
    ): BackendService.TodayStats {
        return BackendService.TodayStats(
            peeCount = 0,
            poopCount = 0,
            feedCount = feedCount,
            lastFeedTimeISO = Instant.ofEpochMilli(
                System.currentTimeMillis() - lastFeedOffsetMinutes * 60_000L,
            ).toString(),
        )
    }

    private fun sleepStats(
        eventType: String,
        eventOffsetMinutes: Long,
        previousSleepDurationLabel: String,
    ): BackendService.TodayStats {
        return BackendService.TodayStats(
            peeCount = 0,
            poopCount = 0,
            feedCount = 0,
            lastFeedTimeISO = null,
            lastSleepEventType = eventType,
            lastSleepEventTimeISO = Instant.ofEpochMilli(
                System.currentTimeMillis() - eventOffsetMinutes * 60_000L,
            ).toString(),
            previousSleepDurationLabel = previousSleepDurationLabel,
        )
    }
}
