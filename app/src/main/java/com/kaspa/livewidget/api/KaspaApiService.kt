package com.kaspa.livewidget.api

import android.util.Log
import com.google.gson.Gson
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.data.BlockRate
import com.kaspa.livewidget.data.HashrateFormatter
import com.kaspa.livewidget.data.NetworkInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service for fetching Kaspa network data from public APIs
 * Uses multiple price data sources with fallback handling
 * Network stats from Kaspa API
 */
class KaspaApiService(
    private val priceSources: List<PriceDataSource> = listOf(
        CoinGeckoPriceDataSource(),
        KaspaExchangePriceDataSource()
    )
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "KaspaApiService"
        
        // API endpoints as constants for easier maintenance and testing
        private const val KASPA_BLOCKDAG_URL = "https://api.kaspa.org/info/blockdag"
        private const val KASPA_HASHRATE_URL = "https://api.kaspa.org/info/hashrate"
        
    }

    /**
     * Fetch current Kaspa price in USD with fallback handling
     * Tries each price source in order until one succeeds
     */
    suspend fun fetchPrice(): Double? {
        for (source in priceSources) {
            try {
                val price = source.fetchPrice()
                if (price != null && price.isFinite() && price > 0) {
                    Log.d(TAG, "Successfully fetched price from ${source.sourceName}: $$price")
                    return price
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from ${source.sourceName}", e)
            }
        }
        Log.e(TAG, "All price sources failed, returning null")
        return null
    }

    /**
     * Fetch network information from Kaspa API
     * Uses api.kaspa.org public endpoint
     */
    suspend fun fetchNetworkInfo(): NetworkInfoResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(KASPA_BLOCKDAG_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        gson.fromJson(body, NetworkInfoResponse::class.java)
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network info unavailable", e)
            null
        }
    }

    /**
     * Fetch hashrate information
     * Kaspa REST /info/hashrate returns TH/s; convert to display units.
     */
    suspend fun fetchHashrate(): String = withContext(Dispatchers.IO) {
        try {
            // Using a public Kaspa explorer API for hashrate
            val request = Request.Builder()
                .url(KASPA_HASHRATE_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        // Parse hashrate from response
                        val hashrate = gson.fromJson(body, Map::class.java)
                        HashrateFormatter.formatThPerSecond(hashrate["hashrate"] as? Double)
                    } ?: "N/A"
                } else "N/A"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hashrate unavailable", e)
            "N/A"
        }
    }

    /**
     * Fetch all Kaspa network data in one call
     */
    suspend fun fetchAllData(previous: KaspaNetworkData?): KaspaNetworkData? = coroutineScope {
        // Independent feeds should not block each other or hide valid readings.
        val priceRequest = async { fetchPrice() }
        val networkRequest = async { fetchNetworkInfo() }
        val hashRequest = async { fetchHashrate() }
        val price = priceRequest.await()
        val networkInfo = networkRequest.await()
        val blockCount = networkInfo?.blockCount
        val blockHeight = networkInfo?.virtualDaaScore
        val networkValid = blockCount != null && blockCount > 0 && blockHeight != null && blockHeight > 0
        if (price == null && !networkValid) return@coroutineScope null

        val hashrate = hashRequest.await()
        val timestamp = System.currentTimeMillis()
        // This is the average number of blocks per second between two API
        // snapshots, never a hard-coded claim about the current network rate.
        val bps = if (networkValid) BlockRate.between(previous, blockCount!!, timestamp) else 0.0

        KaspaNetworkData(
            price = price ?: 0.0,
            blockHeight = if (networkValid) blockHeight!! else 0L,
            bps = bps,
            hashrate = if (networkValid) hashrate else "N/A",
            blockCount = if (networkValid) blockCount!! else 0L,
            timestamp = timestamp
        )
    }

}
