package com.kaspa.livewidget.widget

import com.kaspa.livewidget.data.KaspaNetworkData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object WidgetFreshness {
    private const val FRESH_WINDOW_MS = 30 * 60 * 1000L

    fun label(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L || timestamp > now) return "Waiting for data"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        return if (now - timestamp > FRESH_WINDOW_MS) "Stale since $time" else "Updated $time"
    }

    fun label(data: KaspaNetworkData, now: Long = System.currentTimeMillis()): String {
        val priceTime = data.priceTimestamp.takeIf { it > 0L } ?: data.timestamp
        val networkTime = data.networkTimestamp.takeIf { it > 0L } ?: data.timestamp
        // Report the oldest required reading, never the time of a partial refresh.
        val oldestTime = when {
            data.price <= 0.0 -> networkTime
            data.blockHeight <= 0L -> priceTime
            else -> minOf(priceTime, networkTime)
        }
        val freshness = label(oldestTime, now)
        if (oldestTime <= 0L || oldestTime > now) return freshness
        return when {
            !data.price.isFinite() || data.price <= 0.0 -> "Price unavailable · $freshness"
            data.blockHeight <= 0L -> "Network unavailable · $freshness"
            priceTime <= 0L || priceTime > now || now - priceTime > FRESH_WINDOW_MS -> "Price stale · $freshness"
            networkTime <= 0L || networkTime > now || now - networkTime > FRESH_WINDOW_MS -> "Network stale · $freshness"
            else -> freshness
        }
    }

    fun hashrateLabel(data: KaspaNetworkData, now: Long = System.currentTimeMillis()): String {
        if (data.hashrate.isBlank() || data.hashrate == "N/A") return "Hashrate unavailable"
        return label(data.hashrateTimestamp.takeIf { it > 0L } ?: data.timestamp, now)
    }
}
