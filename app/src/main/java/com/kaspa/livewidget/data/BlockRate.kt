package com.kaspa.livewidget.data

/** Average observed blocks per second between two successful network snapshots. */
object BlockRate {
    fun between(previous: KaspaNetworkData?, blockCount: Long, now: Long): Double {
        if (previous == null || previous.blockCount <= 0 ||
            blockCount < previous.blockCount || previous.timestamp <= 0) return 0.0
        val seconds = (now - previous.timestamp) / 1000.0
        if (seconds !in 60.0..3600.0) return 0.0
        return (blockCount - previous.blockCount) / seconds
    }
}
