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
        .callTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : PriceDataSource {

    override val sourceName: String = "KaspaExchange"

    companion object {
        private const val TAG = "KaspaExchangePriceDataSource"
        
        // Real working Kaspa price API endpoints
        // These provide reliable fallback when CoinGecko is unavailable
        private val FALLBACK_URLS = listOf(
            // KuCoin API - KAS/USDT ticker
            "https://api.kucoin.com/api/v1/market/orderbook/level1?symbol=KAS-USDT",
            // Gate.io API - KAS/USDT ticker
            "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=KAS_USDT"
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
     * Supports KuCoin and Gate.io response structures
     */
    private fun parsePrice(json: String): Double? {
        return try {
            val map = gson.fromJson(json, Map::class.java) as? Map<*, *>
            
            // KuCoin format: {"data": {"price": "0.1234"}}
            val kuCoinData = map?.get("data") as? Map<*, *>
            val kuCoinPrice = (kuCoinData?.get("price") as? String)?.toDoubleOrNull()
            if (kuCoinPrice != null && kuCoinPrice > 0) {
                return kuCoinPrice
            }
            
            // Gate.io format: [{"last": "0.1234"}]
            val gateList = map as? List<*>
            val gateFirst = gateList?.firstOrNull() as? Map<*, *>
            val gatePrice = (gateFirst?.get("last") as? String)?.toDoubleOrNull()
            if (gatePrice != null && gatePrice > 0) {
                return gatePrice
            }
            
            // Generic fallback for other formats
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
