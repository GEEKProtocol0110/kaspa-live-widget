package com.kaspa.livewidget.data

/** Display window is separate from Android's background refresh schedule. */
enum class ChartRange(val key: String, val label: String, val hours: Long, val days: Int) {
    HOUR("1h", "1H · Past hour", 1, 1),
    DAY("24h", "24H · Past day", 24, 1),
    WEEK("7d", "7D · Past week", 168, 7),
    MONTH("30d", "30D · Past 30 days", 720, 30);

    val durationMillis: Long get() = hours * 3_600_000L
    val minimumSpanMillis: Long get() = if (this == HOUR) 50 * 60_000L else durationMillis - 2 * 3_600_000L

    companion object {
        fun fromKey(key: String?) = values().firstOrNull { it.key == key } ?: DAY
    }
}
