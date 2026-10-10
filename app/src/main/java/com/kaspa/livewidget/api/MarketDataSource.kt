package com.kaspa.livewidget.api

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
        "https://api.kucoin.com/api/v1/market/stats?symbol=KAS-USDT",
        "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=KAS_USDT",
        "https://api.kucoin.com/api/v1/market/orderbook/level1?symbol=KAS-USDT"
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
            var priceOnly: MarketQuote? = null
            for (url in exchangeUrls) {
                try {
                    val body = get(url) ?: continue
                    val quote = ExchangeQuoteParser.parse(body, now) ?: continue
                    if (quote.change24h != null) return@withContext quote
                    if (priceOnly == null) priceOnly = quote
                } catch (e: CancellationException) { throw e } catch (_: Exception) { }
            }
            return@withContext priceOnly
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
