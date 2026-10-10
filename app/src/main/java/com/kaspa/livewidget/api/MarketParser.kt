package com.kaspa.livewidget.api

import com.google.gson.JsonParser
import com.kaspa.livewidget.data.MarketQuote
import com.kaspa.livewidget.data.PricePoint
import java.util.Locale

internal object MarketParser {
    fun quote(json: String, currency: String, now: Long): MarketQuote? = try {
        val data = JsonParser.parseString(json).asJsonObject.getAsJsonObject("kaspa")
        val key = currency.lowercase(Locale.US)
        val price = data.get(key)?.asDouble ?: 0.0
        val time = Math.multiplyExact(data.get("last_updated_at")?.asLong ?: 0L, 1000L)
        val change = data.get(key + "_24h_change")?.takeUnless { it.isJsonNull }?.asDouble
            ?.takeIf { it.isFinite() && it >= -100.0 }
        if (!price.isFinite() || price <= 0 || time <= 0 || time > now + 300_000L) null
        else MarketQuote(currency, price, change, time, "CoinGecko")
    } catch (_: Exception) { null }

    fun history(json: String, now: Long): List<PricePoint>? = try {
        val rows = JsonParser.parseString(json).asJsonObject.getAsJsonArray("prices")
        if (rows.size() !in 2..1000) null else {
            val points = rows.map { row ->
                val pair = row.asJsonArray
                require(pair.size() == 2)
                val time = pair[0].asLong
                val price = pair[1].asDouble
                require(time in (now - 26 * 3600_000L)..(now + 300_000L))
                require(price.isFinite() && price > 0)
                PricePoint(time, price)
            }.sortedBy { it.timestamp }.distinctBy { it.timestamp }
            // Require a real day of observations; two nearby points are not a 24h chart.
            if (points.size < 2 || points.last().timestamp < now - 3600_000L ||
                points.last().timestamp - points.first().timestamp < 22 * 3600_000L) null
            else points
        }
    } catch (_: Exception) { null }
}
