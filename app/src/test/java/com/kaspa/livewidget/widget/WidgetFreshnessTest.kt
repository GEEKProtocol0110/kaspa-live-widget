package com.kaspa.livewidget.widget

import com.kaspa.livewidget.data.KaspaNetworkData
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

    @Test fun identifiesPartialReadingsInsteadOfCallingEverythingLive() {
        val now = 3_000_000L
        assertTrue(WidgetFreshness.label(KaspaNetworkData(price = 0.05, timestamp = now), now)
            .startsWith("Network unavailable"))
        assertTrue(WidgetFreshness.label(KaspaNetworkData(blockHeight = 100, timestamp = now), now)
            .startsWith("Price unavailable"))
    }
}
