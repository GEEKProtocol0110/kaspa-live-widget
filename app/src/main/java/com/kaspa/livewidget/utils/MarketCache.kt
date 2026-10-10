package com.kaspa.livewidget.utils

import android.content.Context
import com.google.gson.Gson
import com.kaspa.livewidget.data.MarketData

class MarketCache(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_market_cache", Context.MODE_PRIVATE)
    private val networkCache = DataCache(context)
    private val gson = Gson()

    fun get(currency: String): MarketData {
        require(currency in WidgetPreferences.CURRENCIES)
        val stored = try {
            prefs.getString(currency, null)?.let { gson.fromJson(it, MarketData::class.java) }
        } catch (_: Exception) { null }
        val history: List<com.kaspa.livewidget.data.PricePoint>? = stored?.history
        if (stored != null && stored.currency == currency && history != null &&
            stored.price.isFinite() && stored.price >= 0 && history.size <= 1000 &&
            history.all { it.timestamp > 0 && it.price.isFinite() && it.price > 0 }) return stored
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

    fun save(data: MarketData) {
        require(data.currency in WidgetPreferences.CURRENCIES)
        prefs.edit().putString(data.currency, gson.toJson(data)).apply()
    }
}
