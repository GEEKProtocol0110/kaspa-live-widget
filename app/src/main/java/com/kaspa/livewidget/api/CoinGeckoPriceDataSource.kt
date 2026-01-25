package com.kaspa.livewidget.api

import android.util.Log
import com.google.gson.Gson
import com.kaspa.livewidget.data.PriceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * CoinGecko implementation of PriceDataSource
 * Primary price data source for Kaspa price
 */
class CoinGeckoPriceDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : PriceDataSource {

    override val sourceName: String = "CoinGecko"

    companion object {
        private const val TAG = "CoinGeckoPriceDataSource"
        private const val COINGECKO_PRICE_URL = 
            "https://api.coingecko.com/api/v3/simple/price?ids=kaspa&vs_currencies=usd"
    }

    override suspend fun fetchPrice(): Double? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(COINGECKO_PRICE_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        val priceResponse = gson.fromJson(body, PriceResponse::class.java)
                        priceResponse.kaspa?.usd
                    }
                } else {
                    // Rate limiting typically returns 429
                    if (response.code == 429) {
                        Log.w(TAG, "Rate limited by $sourceName")
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching price from $sourceName", e)
            null
        }
    }
}
