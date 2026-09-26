package com.kaspa.livewidget.data

/**
 * API response models for Kaspa network data
 */
data class PriceResponse(
    val kaspa: PriceData?
)

data class PriceData(
    val usd: Double?
)

data class NetworkInfoResponse(
    val blockCount: Long?,
    val virtualDaaScore: Long?
)
