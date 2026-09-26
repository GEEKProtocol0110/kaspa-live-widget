package com.kaspa.livewidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockRateTest {
    @Test fun usesObservedCountAndElapsedTime() {
        val old = KaspaNetworkData(blockCount = 1200, timestamp = 100_000L)
        assertEquals(2.0, BlockRate.between(old, 1440, 220_000L), 0.0001)
    }

    @Test fun hidesRateWithoutTwoComparableSnapshots() {
        val old = KaspaNetworkData(blockCount = 1200, timestamp = 100_000L)
        assertEquals(0.0, BlockRate.between(null, 1440, 220_000L), 0.0)
        assertEquals(0.0, BlockRate.between(old, 1199, 220_000L), 0.0)
        assertEquals(0.0, BlockRate.between(old, 1440, 120_000L), 0.0)
        assertEquals(0.0, BlockRate.between(old, 1440, 4_000_001L), 0.0)
    }
}
