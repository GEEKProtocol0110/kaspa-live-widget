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
    val difficulty: Long?,
    val headerCount: Long?,
    val tipHashes: List<String>?,
    val virtualParentHashes: List<String>?,
    val virtualDaaScore: Long?
)
