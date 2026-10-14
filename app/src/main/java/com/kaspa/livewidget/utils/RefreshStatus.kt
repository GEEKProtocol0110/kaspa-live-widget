package com.kaspa.livewidget.utils

import android.content.Context

data class RefreshStatus(
    val phase: String = "idle",
    val timestamp: Long = 0L,
    val currency: String = "USD"
) {
    fun inProgress(now: Long = System.currentTimeMillis()) =
        phase in listOf("queued", "running") && timestamp > 0 &&
            now >= timestamp && now - timestamp < 120_000L

    fun label(selectedCurrency: String, now: Long = System.currentTimeMillis()): String {
        if (currency != selectedCurrency && phase !in listOf("idle", "queued"))
            return "Tap to load " + selectedCurrency
        if (phase in listOf("queued", "running") && !inProgress(now)) return "Tap to retry"
        return when (phase) {
            "offline" -> "Offline · saved readings kept · tap to retry"
            "queued" -> "Update queued"
            "running" -> "Refreshing…"
            "complete" -> "Readings updated"
            "partial" -> "Some feeds delayed · cached readings kept"
            "unavailable" -> "Update unavailable · cached readings kept"
            "interrupted" -> "Update interrupted · tap to retry"
            else -> "Tap to refresh"
        }
    }
}

class RefreshStatusCache(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_refresh_status", Context.MODE_PRIVATE)
    fun get() = RefreshStatus(prefs.getString("phase", "idle") ?: "idle",
        prefs.getLong("time", 0L), prefs.getString("currency", "USD") ?: "USD")
    fun set(phase: String, currency: String, now: Long = System.currentTimeMillis()) {
        prefs.edit().putString("phase", phase).putString("currency", currency)
            .putLong("time", now).apply()
    }
}
