package com.kaspa.livewidget.widget

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
}
