package com.kaspa.livewidget.api

/**
 * Interface for fetching Kaspa price data from various sources
 * Allows for multiple implementations and graceful fallback handling
 */
interface PriceDataSource {
    /**
     * Fetch the current Kaspa price in USD
     * @return Price in USD, or null if unavailable
     */
    suspend fun fetchPrice(): Double?
    
    /**
     * Name of the data source for logging/debugging
     */
    val sourceName: String
}
