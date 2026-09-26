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
        val freshness = label(data.timestamp, now)
        if (data.timestamp <= 0L || data.timestamp > now) return freshness
        return when {
            data.price <= 0.0 -> "Price unavailable · $freshness"
            data.blockHeight <= 0L -> "Network unavailable · $freshness"
            else -> freshness
        }
    }
}
