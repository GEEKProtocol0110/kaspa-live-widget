package com.kaspa.livewidget.data

data class PricePoint(val timestamp: Long, val price: Double)

/** All monetary values belong to this currency; never relabel a USD fallback. */
data class MarketData(
    val currency: String = "USD",
    val price: Double = 0.0,
    val change24h: Double? = null,
    val timestamp: Long = 0L,
    val changeTimestamp: Long = 0L,
    val source: String = "",
    val history: List<PricePoint> = emptyList(),
    val historyTimestamp: Long = 0L
)

data class MarketQuote(
    val currency: String,
    val price: Double,
    val change24h: Double?,
    val timestamp: Long,
    val source: String
)

object MarketDataMerger {
    fun merge(old: MarketData?, currency: String, quote: MarketQuote?,
              history: List<PricePoint>?, now: Long): MarketData {
        val prior = old?.takeIf { it.currency == currency } ?: MarketData(currency = currency)
        val valid = quote?.takeIf { it.currency == currency && it.price.isFinite() &&
            it.price > 0 && it.timestamp > 0 && it.timestamp >= prior.timestamp && it.timestamp <= now + 300_000L }
        val change = valid?.change24h?.takeIf { it.isFinite() && it >= -100 }
        // A price-only fallback must not attach an old percentage to a new source.
        return prior.copy(
            price = valid?.price ?: prior.price,
            change24h = if (valid != null) change else prior.change24h,
            timestamp = valid?.timestamp ?: prior.timestamp,
            changeTimestamp = if (valid != null) {
                if (change != null) valid.timestamp else 0L
            } else prior.changeTimestamp,
            source = valid?.source ?: prior.source,
            history = history ?: prior.history,
            historyTimestamp = if (history != null) now else prior.historyTimestamp
        )
    }
}
