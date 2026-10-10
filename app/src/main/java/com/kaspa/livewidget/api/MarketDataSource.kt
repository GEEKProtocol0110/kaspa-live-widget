package com.kaspa.livewidget.api

import com.google.gson.JsonParser
import com.kaspa.livewidget.data.MarketQuote
import com.kaspa.livewidget.data.PricePoint
import com.kaspa.livewidget.utils.WidgetPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

class MarketDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(12, TimeUnit.SECONDS).build(),
    private val coinGeckoBase: String = "https://api.coingecko.com/api/v3",
    private val exchangeUrls: List<String> = listOf(
        "https://api.kucoin.com/api/v1/market/orderbook/level1?symbol=KAS-USDT",
        "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=KAS_USDT"
    )
) {
    private fun get(url: String): String? =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }

    suspend fun quote(currency: String, now: Long): MarketQuote? = withContext(Dispatchers.IO) {
        require(currency in WidgetPreferences.CURRENCIES)
        try {
            val url = coinGeckoBase + "/simple/price?ids=kaspa&vs_currencies=" +
                currency.lowercase(Locale.US) + "&include_24hr_change=true&include_last_updated_at=true"
            get(url)?.let { MarketParser.quote(it, currency, now) }?.let { return@withContext it }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        // KAS/USDT is a USD approximation. It must never be presented as EUR or GBP.
        if (currency == "USD") {
            for (url in exchangeUrls) {
                try {
                    val body = get(url) ?: continue
                    val price = ExchangePriceParser.parse(body) ?: continue
                    val root = JsonParser.parseString(body)
                    val change = if (root.isJsonArray) root.asJsonArray.firstOrNull {
                        it.isJsonObject && it.asJsonObject.get("currency_pair")?.asString == "KAS_USDT"
                    }?.asJsonObject?.get("change_percentage")?.asString?.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it >= -100.0 } else null
                    return@withContext MarketQuote("USD", price, change, now, "KAS/USDT ≈ USD")
                } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
        }
        null
    }

    suspend fun history(currency: String, now: Long): List<PricePoint>? =
        withContext(Dispatchers.IO) {
            require(currency in WidgetPreferences.CURRENCIES)
            try {
                get(coinGeckoBase + "/coins/kaspa/market_chart?vs_currency=" +
                    currency.lowercase(Locale.US) + "&days=1")
                    ?.let { MarketParser.history(it, now) }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        }
}
