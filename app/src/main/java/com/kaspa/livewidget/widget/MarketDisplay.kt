package com.kaspa.livewidget.widget

import com.kaspa.livewidget.data.MarketData
import java.util.Locale

internal object MarketDisplay {
    fun price(data: MarketData): String = if (data.price.isFinite() && data.price > 0)
        String.format(Locale.US, "%s %.4f", data.currency, data.price) else "${data.currency} --"
    fun change(data: MarketData, now: Long = System.currentTimeMillis()): String {
        val value = data.change24h?.takeIf { it.isFinite() && it >= -100 }
        if (value == null || data.changeTimestamp <= 0) return "24h change unavailable"
        val stale = now - data.changeTimestamp > 30 * 60_000L
        return String.format(Locale.US, "24h %+.2f%%%s", value, if (stale) " · stale" else "")
    }
    fun historyLabel(data: MarketData, now: Long = System.currentTimeMillis()): String {
        if (data.history.size < 2) return "24h chart unavailable"
        val stale = now - data.history.last().timestamp > 3_600_000L
        return "24h ${data.currency} chart · ${if (stale) "stale · " else ""}${WidgetFreshness.label(data.historyTimestamp, now)}"
    }
}
