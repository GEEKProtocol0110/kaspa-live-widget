package com.kaspa.livewidget.utils

import android.content.Context

data class WidgetPreferences(
    val currency: String = "USD",
    val intervalMinutes: Long = 15L,
    val theme: String = "dark",
    val showChart: Boolean = false
) {
    fun normalized() = copy(
        currency = currency.takeIf { it in CURRENCIES } ?: "USD",
        intervalMinutes = intervalMinutes.takeIf { it in INTERVALS } ?: 15L,
        theme = theme.takeIf { it in THEMES } ?: "dark"
    )

    companion object {
        val CURRENCIES = listOf("USD", "EUR", "GBP")
        val INTERVALS = listOf(15L, 30L, 60L)
        val THEMES = listOf("dark", "light")
    }
}

class WidgetSettings(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_widget_settings", Context.MODE_PRIVATE)
    fun get() = WidgetPreferences(
        prefs.getString("currency", "USD") ?: "USD",
        prefs.getLong("interval", 15L),
        prefs.getString("theme", "dark") ?: "dark",
        prefs.getBoolean("chart", false)
    ).normalized()

    fun save(value: WidgetPreferences) {
        val safe = value.normalized()
        prefs.edit().putString("currency", safe.currency).putLong("interval", safe.intervalMinutes)
            .putString("theme", safe.theme).putBoolean("chart", safe.showChart).apply()
    }
}
