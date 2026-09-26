package com.kaspa.livewidget.api

import android.util.Log
import com.google.gson.Gson
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.data.BlockRate
import com.kaspa.livewidget.data.NetworkInfoResponse
import kotlinx.coroutines.Dispatchers
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
        KaspaStreamPriceDataSource(),
        KaspaExchangePriceDataSource(),
        CoinGeckoPriceDataSource()
    )
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch hashrate information
     * This is a simplified version - can be enhanced with actual hashrate API
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
                        formatHashrate(hashrate["hashrate"] as? Double ?: 0.0)
                    } ?: "N/A"
                } else "N/A"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "N/A"
        }
    }

    /**
     * Fetch all Kaspa network data in one call
     */
    suspend fun fetchAllData(previous: KaspaNetworkData?): KaspaNetworkData? = withContext(Dispatchers.IO) {
        val price = fetchPrice()
        val networkInfo = fetchNetworkInfo()
        // A failed source must not replace a valid cached reading with zeroes or
        // advance the timestamp. The worker can still show the old data as stale.
        val blockCount = networkInfo?.blockCount
        val blockHeight = networkInfo?.virtualDaaScore
        if (price == null || blockCount == null || blockCount <= 0 ||
            blockHeight == null || blockHeight <= 0) return@withContext null

        val hashrate = fetchHashrate()
        val timestamp = System.currentTimeMillis()
        // This is the average number of blocks per second between two API
        // snapshots, never a hard-coded claim about the current network rate.
        val bps = BlockRate.between(previous, blockCount, timestamp)

        KaspaNetworkData(
            price = price,
            blockHeight = blockHeight,
            bps = bps,
            hashrate = hashrate,
            blockCount = blockCount,
            timestamp = timestamp
        )
    }

    private fun formatHashrate(hashrate: Double): String {
        return when {
            hashrate >= 1_000_000_000_000 -> String.format("%.2f PH/s", hashrate / 1_000_000_000_000)
            hashrate >= 1_000_000_000 -> String.format("%.2f TH/s", hashrate / 1_000_000_000)
            hashrate >= 1_000_000 -> String.format("%.2f GH/s", hashrate / 1_000_000)
            else -> String.format("%.2f MH/s", hashrate / 1_000)
        }
    }
}
