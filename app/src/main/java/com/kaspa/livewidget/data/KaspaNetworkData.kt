package com.kaspa.livewidget.data

/**
 * Data class representing Kaspa network information
 */
data class KaspaNetworkData(
    val price: Double = 0.0,
    val blockHeight: Long = 0L,
    val bps: Double = 0.0,
    val hashrate: String = "N/A",
    // Cumulative count lets us calculate an average across successful snapshots.
    val blockCount: Long = 0L,
    val timestamp: Long = 0L
)
