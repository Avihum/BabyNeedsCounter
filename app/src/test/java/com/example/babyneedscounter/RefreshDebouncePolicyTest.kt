package com.example.babyneedscounter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshDebouncePolicyTest {
    @Test
    fun widgetRefreshIsDebouncedInsideWindow() {
        assertTrue(
            RefreshDebouncePolicy.shouldDebounce(
                trigger = RefreshTrigger.Widget,
                lastAttemptAt = 5_000L,
                nowMs = 64_999L,
            )
        )
    }

    @Test
    fun widgetRefreshRunsAgainAfterWindowExpires() {
        assertFalse(
            RefreshDebouncePolicy.shouldDebounce(
                trigger = RefreshTrigger.Widget,
                lastAttemptAt = 5_000L,
                nowMs = 65_000L,
            )
        )
    }

    @Test
    fun resumeRefreshUsesSameDebounceWindow() {
        assertTrue(
            RefreshDebouncePolicy.shouldDebounce(
                trigger = RefreshTrigger.Resume,
                lastAttemptAt = 100L,
                nowMs = 59_999L,
            )
        )
    }

    @Test
    fun manualRefreshIsNeverDebounced() {
        assertFalse(
            RefreshDebouncePolicy.shouldDebounce(
                trigger = RefreshTrigger.Manual,
                lastAttemptAt = 100L,
                nowMs = 101L,
            )
        )
    }
}
