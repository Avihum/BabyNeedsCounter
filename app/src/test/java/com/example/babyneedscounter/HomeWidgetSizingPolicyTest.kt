package com.example.babyneedscounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeWidgetSizingPolicyTest {
    @Test
    fun measurementSample_preservesFormatAndUsesWidestDigits() {
        assertEquals("88:88", HomeWidgetSizingPolicy.measurementSample("18:37"))
        assertEquals("8h 88m", HomeWidgetSizingPolicy.measurementSample("2h 10m"))
        assertEquals("Open app", HomeWidgetSizingPolicy.measurementSample("Open app"))
    }

    @Test
    fun smallWidgets_measureOnlyTheRightContentArea() {
        val contentBox = HomeWidgetSizingPolicy.contentBox(
            kind = HomeWidgetKind.FEED_SMALL,
            boundsDp = WidgetBoundsDp(widthDp = 120f, heightDp = 56f),
        )

        assertEquals(67.8f, contentBox.widthDp, 0.2f)
        assertEquals(38f, contentBox.heightDp, 0.01f)
    }

    @Test
    fun largeWidgets_keepMoreRoomForCenteredPrimaryValue() {
        val contentBox = HomeWidgetSizingPolicy.contentBox(
            kind = HomeWidgetKind.SLEEP_LARGE,
            boundsDp = WidgetBoundsDp(widthDp = 250f, heightDp = 110f),
        )

        assertEquals(170.1f, contentBox.widthDp, 0.2f)
        assertEquals(86f, contentBox.heightDp, 0.01f)
        assertTrue(contentBox.widthDp > contentBox.heightDp)
    }
}
