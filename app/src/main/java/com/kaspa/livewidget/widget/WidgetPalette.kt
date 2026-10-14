package com.kaspa.livewidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.kaspa.livewidget.R
import com.kaspa.livewidget.utils.WidgetPreferences

/** One palette shared by both launcher sizes and the unsaved setup preview. */
internal data class WidgetPalette(val background: Int, val text: Int, val accent: Int,
    val secondary: Int, val personalized: Boolean,
    val clock: Int = accent, val price: Int = accent, val chart: Int = accent, val network: Int = text) {
    fun logo(context: Context): Bitmap {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        context.getDrawable(R.drawable.kaspa_mark)!!.apply {
            setBounds(0, 0, 64, 64); draw(Canvas(image))
        }
        // Recolor the mark's teal silhouette and white cutout separately so its
        // shape stays recognizable, rather than tinting it into a solid disk.
        val pixels = IntArray(64 * 64)
        image.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            if ((pixel ushr 24) == 0) continue
            val amount = ((255 - ((pixel ushr 16) and 255)) / 144f).coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val start = (background ushr shift) and 255
                val end = (accent ushr shift) and 255
                return (start + (end - start) * amount).toInt()
            }
            pixels[i] = (pixel and 0xFF000000.toInt()) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        image.setPixels(pixels, 0, 64, 0, 0, 64, 64)
        return image
    }

    companion object {
        fun from(prefs: WidgetPreferences): WidgetPalette = when (prefs.theme) {
            "light" -> WidgetPalette(0xFFF1FBFA.toInt(), Color.rgb(12,43,55), Color.rgb(9,86,91), Color.rgb(47,77,85), false)
            "xray" -> WidgetPalette(Color.BLACK, Color.WHITE, Color.WHITE, 0xFFD0D0D0.toInt(), true)
            "custom" -> prefs.customColors.normalized().let {
                WidgetPalette(it.background, it.text, it.accent,
                    ColorUtils.blendARGB(it.background, it.text, .85f), true,
                    it.clock ?: it.accent, it.price ?: it.accent, it.chart ?: it.accent, it.network ?: it.text)
            }
            else -> WidgetPalette(0xFF050C18.toInt(), Color.rgb(222,255,249), Color.rgb(120,255,224), Color.rgb(181,218,219), false)
        }
    }
}
