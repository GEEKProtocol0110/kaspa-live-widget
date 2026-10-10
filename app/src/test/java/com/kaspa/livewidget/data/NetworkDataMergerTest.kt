package com.kaspa.livewidget.data

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class NetworkDataMergerTest {
    private val old = KaspaNetworkData(
        price = 0.03, blockHeight = 9000, blockCount = 1200, bps = 2.0,
        hashrate = "400 PH/s", timestamp = 100_000L
    )

    @Test fun priceOnlyRefreshKeepsNetworkAndItsOriginalClock() {
        val updated = NetworkDataMerger.merge(old, 0.04, null, "N/A", 160_000L)!!
        assertEquals(0.04, updated.price, 0.0)
        assertEquals(9000L, updated.blockHeight)
        assertEquals(2.0, updated.bps, 0.0)
        assertEquals("400 PH/s", updated.hashrate)
        assertEquals(100_000L, updated.networkTimestamp)
        assertEquals(100_000L, updated.hashrateTimestamp)
        assertEquals(160_000L, updated.priceTimestamp)
        // The price refresh must not shorten the network observation interval.
        val recovered = NetworkDataMerger.merge(updated, null, NetworkInfoResponse(1440, 9200), "N/A", 220_000L)!!
        assertEquals(2.0, recovered.bps, 0.0001)
        assertEquals(160_000L, recovered.priceTimestamp)
        assertEquals(220_000L, recovered.networkTimestamp)
    }

    @Test fun networkOnlyAndHashOnlyUpdatesKeepOtherFeeds() {
        val networkOnly = NetworkDataMerger.merge(old, null, NetworkInfoResponse(1440, 9200), "N/A", 220_000L)!!
        assertEquals(0.03, networkOnly.price, 0.0)
        assertEquals(100_000L, networkOnly.priceTimestamp)
        val hashOnly = NetworkDataMerger.merge(networkOnly, null, null, "410 PH/s", 230_000L)!!
        assertEquals(220_000L, hashOnly.networkTimestamp)
        assertEquals(100_000L, hashOnly.priceTimestamp)
        assertEquals(230_000L, hashOnly.hashrateTimestamp)
    }

    @Test fun totalFailureCannotRenewCacheAndInvalidReadingsCannotEraseIt() {
        assertNull(NetworkDataMerger.merge(old, null, null, "N/A", 220_000L))
        assertNull(NetworkDataMerger.merge(old, Double.NaN, NetworkInfoResponse(-1, 9200), "", 220_000L))
        val updated = NetworkDataMerger.merge(old, Double.POSITIVE_INFINITY, NetworkInfoResponse(1440, 9200), "N/A", 220_000L)!!
        assertEquals(old.price, updated.price, 0.0)
        assertEquals(100_000L, updated.priceTimestamp)
    }

    @Test fun legacyJsonSurvivesWithoutFabricatedFreshness() {
        val legacy = Gson().fromJson("""{"price":0.03,"blockHeight":9000,"blockCount":1200,"bps":2,"hashrate":"400 PH/s","timestamp":100000}""", KaspaNetworkData::class.java)
        val updated = NetworkDataMerger.merge(legacy, 0.04, null, "N/A", 220_000L)!!
        assertEquals(100_000L, updated.networkTimestamp)
        assertEquals(100_000L, updated.hashrateTimestamp)
        val restored = Gson().fromJson(Gson().toJson(updated), KaspaNetworkData::class.java)
        assertEquals(updated, restored)
    }

    @Test fun firstPartialReadingLeavesUnavailableFeedsUnavailable() {
        val first = NetworkDataMerger.merge(null, null, NetworkInfoResponse(1200, 9000), "N/A", 100_000L)!!
        assertEquals(0.0, first.price, 0.0)
        assertEquals(0L, first.priceTimestamp)
        assertEquals(0.0, first.bps, 0.0)
    }
}
