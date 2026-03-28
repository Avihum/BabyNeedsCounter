package com.example.babyneedscounter

data class FeedSmallWidgetModel(
    val label: String,
    val primaryText: String,
)

data class FeedLargeWidgetModel(
    val title: String,
    val primaryText: String,
    val countdownText: String,
    val lastFeedText: String,
    val countText: String,
    val actionLabel: String,
)

data class SleepSmallWidgetModel(
    val state: String,
    val durationText: String,
    val iconRes: Int,
)

data class SleepLargeWidgetModel(
    val state: String,
    val durationText: String,
    val previousSleepText: String,
    val actionLabel: String,
    val iconRes: Int,
)

object WidgetPresentationMapper {
    fun feedSmall(snapshot: WidgetHomeSnapshot): FeedSmallWidgetModel {
        val stats = snapshot.stats
        val nextFeed = stats?.getNextFeedTime().orMeaningful()

        return when {
            nextFeed != null -> FeedSmallWidgetModel(
                label = "Next feed",
                primaryText = nextFeed,
            )
            snapshot.hasCachedContent -> FeedSmallWidgetModel(
                label = "Next feed",
                primaryText = "No feed yet",
            )
            else -> FeedSmallWidgetModel(
                label = "Feed",
                primaryText = "Open app",
            )
        }
    }

    fun feedLarge(snapshot: WidgetHomeSnapshot): FeedLargeWidgetModel {
        val stats = snapshot.stats
        val nextFeed = stats?.getNextFeedTime().orMeaningful()
        val nextIn = stats?.getTimeUntilNextFeed().orMeaningful()
        val lastFeedAgo = stats?.getTimeSinceLastFeed().orMeaningful()
        val feedCount = stats?.feedCount ?: 0

        return when {
            nextFeed != null -> FeedLargeWidgetModel(
                title = "Next feed",
                primaryText = nextFeed,
                countdownText = when {
                    nextIn == null -> "Due time unavailable"
                    nextIn.equals("Now!", ignoreCase = true) -> "Due now"
                    else -> "In $nextIn"
                },
                lastFeedText = when {
                    lastFeedAgo == null -> "Last feed unavailable"
                    lastFeedAgo.equals("Just now", ignoreCase = true) -> "Last feed just now"
                    else -> "Last feed $lastFeedAgo ago"
                },
                countText = "$feedCount today",
                actionLabel = "Log",
            )
            snapshot.hasCachedContent -> FeedLargeWidgetModel(
                title = "Next feed",
                primaryText = "No feed yet",
                countdownText = "Log the first feed in app",
                lastFeedText = "Last feed unavailable",
                countText = "$feedCount today",
                actionLabel = "Log",
            )
            else -> FeedLargeWidgetModel(
                title = "Feed",
                primaryText = "Open app to sync",
                countdownText = "Last saved data will appear here",
                lastFeedText = "",
                countText = "",
                actionLabel = "Log",
            )
        }
    }

    fun sleepSmall(snapshot: WidgetHomeSnapshot): SleepSmallWidgetModel {
        val stats = snapshot.stats
        val sleeping = stats?.isSleeping() == true
        val awake = stats?.isAwakeFromLastSleepEvent() == true
        val duration = sleepDurationPrimaryLine(stats, sleeping, awake).orMeaningful()

        return when {
            sleeping || awake -> SleepSmallWidgetModel(
                state = if (sleeping) "Sleeping" else "Awake",
                durationText = duration ?: if (sleeping) "Sleeping" else "Awake",
                iconRes = if (sleeping) R.drawable.marshmallow_sleep else R.drawable.marshmallow_awake,
            )
            snapshot.hasCachedContent -> SleepSmallWidgetModel(
                state = "Sleep",
                durationText = "No sleep yet",
                iconRes = R.drawable.marshmallow_awake,
            )
            else -> SleepSmallWidgetModel(
                state = "Sleep",
                durationText = "Open app",
                iconRes = R.drawable.marshmallow_awake,
            )
        }
    }

    fun sleepLarge(snapshot: WidgetHomeSnapshot): SleepLargeWidgetModel {
        val stats = snapshot.stats
        val sleeping = stats?.isSleeping() == true
        val awake = stats?.isAwakeFromLastSleepEvent() == true
        val duration = sleepDurationPrimaryLine(stats, sleeping, awake).orMeaningful()
        val previousSleep = stats?.previousSleepDurationLabel
            ?.takeIf { it.isNotBlank() }
            ?.let(::formatPreviousSleepLine)

        return when {
            sleeping || awake -> SleepLargeWidgetModel(
                state = if (sleeping) "Sleeping" else "Awake",
                durationText = duration ?: if (sleeping) "Sleeping" else "Awake",
                previousSleepText = previousSleep ?: "Previous sleep cycle unavailable",
                actionLabel = if (sleeping) "Stop" else "Start",
                iconRes = if (sleeping) R.drawable.marshmallow_sleep else R.drawable.marshmallow_awake,
            )
            snapshot.hasCachedContent -> SleepLargeWidgetModel(
                state = "Sleep",
                durationText = "No sleep yet",
                previousSleepText = "Previous sleep cycle unavailable",
                actionLabel = "Start",
                iconRes = R.drawable.marshmallow_awake,
            )
            else -> SleepLargeWidgetModel(
                state = "Sleep",
                durationText = "Open app to sync",
                previousSleepText = "Last saved sleep data will appear here",
                actionLabel = "Start",
                iconRes = R.drawable.marshmallow_awake,
            )
        }
    }
}

private fun String?.orMeaningful(): String? {
    return this?.takeIf {
        it.isNotBlank() &&
            it != "—" &&
            it != "--" &&
            it != "00"
    }
}
