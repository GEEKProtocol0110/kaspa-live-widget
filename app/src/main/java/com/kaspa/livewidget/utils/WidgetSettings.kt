package com.kaspa.livewidget.utils

import android.content.Context

data class WidgetColors(
    val background: Int = 0xFF050C18.toInt(),
    val text: Int = 0xFFDEFFF9.toInt(),
    val accent: Int = 0xFF78FFE0.toInt(),
    val clock: Int? = null,
    val price: Int? = null,
    val chart: Int? = null,
    val network: Int? = null
) {
    fun normalized() = copy(background = background or 0xFF000000.toInt(),
        text = text or 0xFF000000.toInt(), accent = accent or 0xFF000000.toInt(),
        clock = clock?.or(0xFF000000.toInt()), price = price?.or(0xFF000000.toInt()),
        chart = chart?.or(0xFF000000.toInt()), network = network?.or(0xFF000000.toInt()))
}

data class WidgetPreferences(
    val currency: String = "USD",
    val intervalMinutes: Long = 15L,
    val theme: String = "dark",
    val showChart: Boolean = true,
    val customColors: WidgetColors = WidgetColors(),
    val chartIntensity: Int = 50
) {
    fun normalized() = copy(
        currency = currency.takeIf { it in CURRENCIES } ?: "USD",
        intervalMinutes = intervalMinutes.takeIf { it in INTERVALS } ?: 15L,
        theme = theme.takeIf { it in THEMES } ?: "dark",
        customColors = customColors.normalized(),
        chartIntensity = chartIntensity.coerceIn(0, 100)
    )

    companion object {
        val CURRENCIES = listOf("USD", "EUR", "GBP")
        val INTERVALS = listOf(15L, 30L, 60L)
        val THEMES = listOf("dark", "light", "xray", "custom")
    }
}

class WidgetSettings(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_widget_settings", Context.MODE_PRIVATE)
    fun get() = WidgetPreferences(
        prefs.getString("currency", "USD") ?: "USD",
        prefs.getLong("interval", 15L),
        prefs.getString("theme", "dark") ?: "dark",
        prefs.getBoolean("chart", true),
        WidgetColors(prefs.getInt("color_background", WidgetColors().background),
            prefs.getInt("color_text", WidgetColors().text), prefs.getInt("color_accent", WidgetColors().accent),
            optionalColor("clock"), optionalColor("price"), optionalColor("chart"), optionalColor("network")),
        prefs.getInt("chart_intensity", 50)
    ).normalized()

    fun save(value: WidgetPreferences) {
        val safe = value.normalized()
        prefs.edit().putString("currency", safe.currency).putLong("interval", safe.intervalMinutes)
            .putString("theme", safe.theme).putBoolean("chart", safe.showChart)
            .putInt("color_background", safe.customColors.background).putInt("color_text", safe.customColors.text)
            .putInt("color_accent", safe.customColors.accent).putInt("chart_intensity", safe.chartIntensity)
            .apply {
                for ((key, color) in listOf("clock" to safe.customColors.clock, "price" to safe.customColors.price,
                    "chart" to safe.customColors.chart, "network" to safe.customColors.network)) {
                    if (color == null) remove("color_$key") else putInt("color_$key", color)
                }
            }.apply()
    }
    private fun optionalColor(name: String): Int? =
        if (prefs.contains("color_$name")) prefs.getInt("color_$name", 0) else null
}
