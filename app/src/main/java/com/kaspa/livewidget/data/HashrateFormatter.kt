package com.kaspa.livewidget.data

import java.util.Locale

/** /info/hashrate reports terahashes per second, not hashes per second. */
object HashrateFormatter {
    fun formatThPerSecond(value: Double?): String {
        if (value == null || !value.isFinite() || value <= 0.0) return "N/A"
        return when {
            value >= 1_000_000 -> String.format(Locale.US, "%.2f EH/s", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.2f PH/s", value / 1_000)
            else -> String.format(Locale.US, "%.2f TH/s", value)
        }
    }
}
