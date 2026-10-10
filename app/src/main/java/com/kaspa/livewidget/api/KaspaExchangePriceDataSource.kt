package com.kaspa.livewidget.api

import android.util.Log
import kotlinx.coroutines.CancellationException
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
        .build()
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
                            val price = ExchangePriceParser.parse(body)
                            if (price != null && price > 0) {
                                return@withContext price
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Continue to next URL
                Log.w(TAG, "Error fetching from $url", e)
            }
        }
        null
    }

}
