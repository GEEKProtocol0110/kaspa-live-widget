package com.kaspa.livewidget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFreshnessTest {
    @Test fun neverLabelsMissingOrOldDataAsRecentlyUpdated() {
        val now = 3_000_000L
        assertEquals("Waiting for data", WidgetFreshness.label(0L, now))
        assertTrue(WidgetFreshness.label(now - 31 * 60_000L, now).startsWith("Stale since "))
        assertTrue(WidgetFreshness.label(now - 15 * 60_000L, now).startsWith("Updated "))
    }
}
