package com.kaspa.livewidget.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local appearance presets never change a currency or start a refresh. */
data class WidgetPreset(val name: String, val theme: String, val colors: WidgetColors, val intensity: Int) {
    fun applyTo(preferences: WidgetPreferences) = preferences.copy(theme = theme,
        customColors = colors, chartIntensity = intensity).normalized()
}

class WidgetPresets(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_widget_presets", Context.MODE_PRIVATE)
    fun all(): List<WidgetPreset> = runCatching {
        val entries = JSONArray(prefs.getString("styles", "[]"))
        (0 until minOf(entries.length(), 20)).mapNotNull { index -> runCatching {
            val item = entries.getJSONObject(index)
            val name = item.getString("name").trim()
            require(name.isNotEmpty() && name.length <= 32)
            val theme = item.getString("theme"); require(theme in WidgetPreferences.THEMES)
            val colors = item.getJSONObject("colors")
            fun optional(key: String) = if (colors.has(key)) colors.getInt(key) else null
            WidgetPreset(name, theme, WidgetColors(colors.getInt("background"), colors.getInt("text"),
                colors.getInt("accent"), optional("clock"), optional("price"), optional("chart"), optional("network")).normalized(),
                item.getInt("intensity").coerceIn(0, 100))
        }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(name: String, value: WidgetPreferences): Boolean {
        val title = name.trim()
        if (title.isEmpty() || title.length > 32) return false
        val entries = all().toMutableList()
        val index = entries.indexOfFirst { it.name.equals(title, ignoreCase = true) }
        val safe = value.normalized()
        val preset = WidgetPreset(title, safe.theme, safe.customColors, safe.chartIntensity)
        if (index >= 0) entries[index] = preset else {
            if (entries.size >= 20) return false
            entries.add(preset)
        }
        write(entries); return true
    }
    fun delete(name: String) = write(all().filterNot { it.name == name })
    private fun write(entries: List<WidgetPreset>) {
        val array = JSONArray()
        entries.forEach { preset ->
            val c = preset.colors
            val colors = JSONObject().put("background", c.background).put("text", c.text).put("accent", c.accent)
            for ((key, value) in listOf("clock" to c.clock, "price" to c.price, "chart" to c.chart, "network" to c.network))
                if (value != null) colors.put(key, value)
            array.put(JSONObject().put("name", preset.name).put("theme", preset.theme)
                .put("intensity", preset.intensity).put("colors", colors))
        }
        prefs.edit().putString("styles", array.toString()).apply()
    }
}
