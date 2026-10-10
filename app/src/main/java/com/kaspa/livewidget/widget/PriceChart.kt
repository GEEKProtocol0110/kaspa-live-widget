package com.kaspa.livewidget.widget

import android.graphics.*
import com.kaspa.livewidget.data.PricePoint

internal object PriceChart {
    fun bitmap(points: List<PricePoint>, color: Int, width: Int = 600, height: Int = 100): Bitmap {
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val ordered = points.filter { it.price.isFinite() && it.price > 0 }.sortedBy { it.timestamp }
        if (ordered.size < 2 || ordered.last().timestamp <= ordered.first().timestamp) return image
        val low = ordered.minOf { it.price }
        val high = ordered.maxOf { it.price }
        val range = (high - low).takeIf { it > 0 } ?: low * .01
        val path = Path()
        ordered.forEachIndexed { index, p ->
            val x = 4f + ((p.timestamp - ordered.first().timestamp).toDouble() /
                (ordered.last().timestamp - ordered.first().timestamp) * (width - 8)).toFloat()
            val y = if (high == low) height / 2f else 4f + ((high - p.price) / range * (height - 8)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        Canvas(image).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; strokeWidth = 3f; style = Paint.Style.STROKE
        })
        return image
    }
}
