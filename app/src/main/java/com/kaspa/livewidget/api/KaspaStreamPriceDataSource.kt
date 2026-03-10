package com.kaspa.livewidget.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * kaspa.stream implementation of PriceDataSource.
 * Tries likely public market endpoints and extracts KAS/USD price.
 */
class KaspaStreamPriceDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) : PriceDataSource {

    override val sourceName: String = "kaspa.stream"

    companion object {
        private const val TAG = "KaspaStreamPriceDataSource"

        // kaspa.stream routes currently observed in production
        private val PRICE_URLS = listOf(
            "https://kaspa.stream/api/v1/price",
            "https://kaspa.stream/api/price",
            "https://kaspa.stream/api/v1/market",
            "https://kaspa.stream/api/market"
        )

        private val HTML_PRICE_REGEXES = listOf(
            Regex("\"price\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE),
            Regex("\"usd\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE),
            Regex("\\$\\s*([0-9]+(?:\\.[0-9]+)?)")
        )
    }

    override suspend fun fetchPrice(): Double? = withContext(Dispatchers.IO) {
        for (url in PRICE_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json, text/plain, */*")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        continue
                    }

                    val body = response.body?.string() ?: continue
                    val parsed = parsePrice(body)
                    if (parsed != null && parsed > 0) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching from $url", e)
            }
        }

        null
    }

    private fun parsePrice(raw: String): Double? {
        parseJsonPrice(raw)?.let { return it }

        for (regex in HTML_PRICE_REGEXES) {
            val value = regex.find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
            if (value != null && value > 0) {
                return value
            }
        }

        return null
    }

    private fun parseJsonPrice(raw: String): Double? {
        return try {
            val json = gson.fromJson(raw, Any::class.java)
            extractPrice(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPrice(node: Any?): Double? {
        return when (node) {
            is Number -> node.toDouble().takeIf { it > 0 }
            is String -> node.toDoubleOrNull()?.takeIf { it > 0 }
            is Map<*, *> -> {
                val keyCandidates = listOf(
                    "price", "usd", "priceUsd", "last", "kaspaUsd", "kas_usd"
                )

                for (key in keyCandidates) {
                    val direct = extractPrice(node[key])
                    if (direct != null) {
                        return direct
                    }
                }

                // Deep scan as a fallback when shape is unknown.
                node.values.asSequence()
                    .mapNotNull { extractPrice(it) }
                    .firstOrNull()
            }
            is List<*> -> node.asSequence()
                .mapNotNull { extractPrice(it) }
                .firstOrNull()
            else -> null
        }
    }
}
