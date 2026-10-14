package com.kaspa.livewidget.widget

import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.data.MarketData
import com.kaspa.livewidget.utils.RefreshStatus

internal object RefreshFeedback {
    fun caption(status: RefreshStatus, market: MarketData, data: KaspaNetworkData,
                offline: Boolean, now: Long = System.currentTimeMillis()): String {
        if (status.inProgress(now)) return status.label(market.currency, now)
        if (offline) return "Offline · retry"
        if (status.phase == "offline") return "Tap to retry"
        val phase = status.label(market.currency, now)
        if (status.phase in listOf("queued", "running", "partial", "unavailable", "interrupted") || status.currency != market.currency)
            return when (phase) {
                "Some feeds delayed · cached readings kept" -> "Some feeds delayed"
                "Update unavailable · cached readings kept" -> "Update unavailable"
                "Update interrupted · tap to retry" -> "Tap to retry"
                else -> phase
            }
        val networkTime = data.networkTimestamp.takeIf { it > 0 } ?: data.timestamp
        return when {
            market.timestamp <= 0 || market.timestamp > now || !market.price.isFinite() || market.price <= 0 -> "Waiting for price"
            now - market.timestamp > 30 * 60_000L -> "Price stale"
            data.blockHeight <= 0 || networkTime <= 0 || networkTime > now -> "Waiting for network"
            now - networkTime > 30 * 60_000L -> "Network stale"
            else -> phase
        }
    }
    fun ages(market: MarketData, data: KaspaNetworkData, now: Long = System.currentTimeMillis()): String {
        fun age(timestamp: Long) = if (timestamp <= 0 || timestamp > now) "not saved" else {
            val minutes = (now - timestamp) / 60_000
            if (minutes == 0L) "less than a minute" else "$minutes minute${if (minutes == 1L) "" else "s"}"
        }
        return "Price age: ${age(market.timestamp)}\nNetwork age: ${age(data.networkTimestamp.takeIf { it > 0 } ?: data.timestamp)}"
    }
}
