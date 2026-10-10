package com.kaspa.livewidget.api

import com.google.gson.JsonParser
import com.kaspa.livewidget.data.MarketQuote

/** Public spot quotes only; a KuCoin rate is a fraction, Gate reports percent. */
internal object ExchangeQuoteParser {
    fun parse(json: String, now: Long): MarketQuote? = try {
        val root = JsonParser.parseString(json)
        val source = "KAS/USDT ≈ USD"
        when {
            root.isJsonObject -> {
                val envelope = root.asJsonObject
                require(envelope.get("code")?.asString == "200000")
                val data = envelope.getAsJsonObject("data")
                if (data.has("last")) {
                    require(data.get("symbol")?.asString == "KAS-USDT")
                    val time = data.get("time")?.asLong ?: 0L
                    require(time > 0 && time <= now + 300_000L && time >= now - 30 * 60_000L)
                    val price = data.get("last")?.asString?.toDoubleOrNull() ?: 0.0
                    val change = data.get("changeRate")?.takeUnless { it.isJsonNull }
                        ?.asString?.toDoubleOrNull()?.times(100)
                        ?.takeIf { it.isFinite() && it >= -100 }
                    require(price.isFinite() && price > 0)
                    MarketQuote("USD", price, change, time, source)
                } else ExchangePriceParser.parse(json)?.let { MarketQuote("USD", it, null, now, source) }
            }
            root.isJsonArray -> {
                val data = root.asJsonArray.firstOrNull {
                    it.isJsonObject && it.asJsonObject.get("currency_pair")?.asString == "KAS_USDT"
                }?.asJsonObject
                val price = ExchangePriceParser.parse(json)
                val change = data?.get("change_percentage")?.takeUnless { it.isJsonNull }
                    ?.asString?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= -100 }
                price?.let { MarketQuote("USD", it, change, now, source) }
            }
            else -> null
        }
    } catch (_: Exception) { null }
}
