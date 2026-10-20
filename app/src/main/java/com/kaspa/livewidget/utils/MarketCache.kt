package com.kaspa.livewidget.utils

import android.content.Context
import com.google.gson.Gson
import com.kaspa.livewidget.data.MarketData
import com.kaspa.livewidget.data.ChartRange

class MarketCache(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_market_cache", Context.MODE_PRIVATE)
    private val networkCache = DataCache(context)
    private val gson = Gson()

    fun get(currency: String, range: ChartRange = ChartRange.DAY): MarketData {
        val quote = read(currency, currency) ?: legacy(currency)
        if (range == ChartRange.DAY) return quote
        val history = read("${currency}_history_${range.key}", currency)
        return quote.copy(history = history?.history ?: emptyList(), historyTimestamp = history?.historyTimestamp ?: 0L)
    }

    private fun read(key: String, currency: String): MarketData? {
        require(currency in WidgetPreferences.CURRENCIES)
        val stored = try {
            prefs.getString(key, null)?.let { gson.fromJson(it, MarketData::class.java) }
        } catch (_: Exception) { null }
        val history: List<com.kaspa.livewidget.data.PricePoint>? = stored?.history
        if (stored != null && stored.currency == currency && history != null &&
            stored.price.isFinite() && stored.price >= 0 && history.size <= 1000 &&
            history.all { it.timestamp > 0 && it.price.isFinite() && it.price > 0 }) return stored
        return null
    }

    private fun legacy(currency: String): MarketData {
        if (currency == "USD") {
            val legacy = networkCache.getCachedData()
            if (legacy != null && legacy.price.isFinite() && legacy.price > 0) {
                return MarketData(price = legacy.price,
                    timestamp = legacy.priceTimestamp.takeIf { it > 0 } ?: legacy.timestamp,
                    source = "Previously saved USD reading")
            }
        }
        return MarketData(currency = currency)
    }

    fun save(data: MarketData, range: ChartRange = ChartRange.DAY) {
        require(data.currency in WidgetPreferences.CURRENCIES)
        val daily = if (range == ChartRange.DAY) data else {
            val old = get(data.currency)
            data.copy(history = old.history, historyTimestamp = old.historyTimestamp)
        }
        // Share the latest quote across ranges, preserving the legacy 24h series.
        prefs.edit().putString(data.currency, gson.toJson(daily)).apply {
            if (range != ChartRange.DAY) putString("${data.currency}_history_${range.key}", gson.toJson(data))
        }.apply()
    }
}
