package com.kaspa.livewidget.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Alternative price data source using Kaspa exchange APIs
 * Serves as fallback when primary source is unavailable
 */
class KaspaExchangePriceDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : PriceDataSource {

    override val sourceName: String = "KaspaExchange"

    companion object {
        private const val TAG = "KaspaExchangePriceDataSource"
        
        // TODO: Replace these with actual working Kaspa price API endpoints
        // These are example endpoints and may not be functional
        // Consider using: KuCoin, Gate.io, or other exchanges with Kaspa listings
        private val FALLBACK_URLS = listOf(
            "https://api.kaspa.org/info/price",
            "https://kaspa.org/api/price"
        )
    }

    override suspend fun fetchPrice(): Double? = withContext(Dispatchers.IO) {
        // Try each fallback URL in order
        for (url in FALLBACK_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { body ->
                            // Attempt to parse different response formats
                            val price = parsePrice(body)
                            if (price != null && price > 0) {
                                return@withContext price
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next URL
                Log.w(TAG, "Error fetching from $url", e)
            }
        }
        null
    }

    /**
     * Parse price from various JSON response formats
     */
    private fun parsePrice(json: String): Double? {
        return try {
            // Try to parse as direct price object with proper type safety
            val map = gson.fromJson(json, Map::class.java) as? Map<*, *>
            
            // Try common field names
            (map?.get("price") as? Number)?.toDouble()
                ?: (map?.get("usd") as? Number)?.toDouble()
                ?: (map?.get("priceUsd") as? Number)?.toDouble()
                ?: (map?.get("last") as? Number)?.toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing price from JSON", e)
            null
        }
    }
}
