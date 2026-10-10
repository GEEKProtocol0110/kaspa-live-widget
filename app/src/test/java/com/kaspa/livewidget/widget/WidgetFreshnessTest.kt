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

    @Test fun freshNetworkDoesNotRelabelAnOldPrice() {
        val now = 3_000_000L
        val data = KaspaNetworkData(price = 0.05, blockHeight = 100, timestamp = now,
            priceTimestamp = now - 31 * 60_000L, networkTimestamp = now)
        assertTrue(WidgetFreshness.label(data, now).startsWith("Price stale · Stale since "))
        assertTrue(WidgetFreshness.label(data.copy(priceTimestamp = now,
            networkTimestamp = now - 31 * 60_000L), now).startsWith("Network stale · Stale since "))
    }

    @Test fun hashrateHasItsOwnFreshnessAndMissingValuesStayUnavailable() {
        val now = 3_000_000L
        val data = KaspaNetworkData(hashrate = "400 PH/s", timestamp = now,
            hashrateTimestamp = now - 31 * 60_000L)
        assertTrue(WidgetFreshness.hashrateLabel(data, now).startsWith("Stale since "))
        assertEquals("Hashrate unavailable", WidgetFreshness.hashrateLabel(data.copy(hashrate = "N/A"), now))
        assertTrue(WidgetFreshness.hashrateLabel(data.copy(hashrateTimestamp = 0L), now).startsWith("Updated "))
    }
}
