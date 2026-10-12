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
    val secondary: Int, val personalized: Boolean) {
    fun logo(context: Context): Bitmap {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        context.getDrawable(R.drawable.kaspa_mark)!!.apply {
            setBounds(0, 0, 64, 64); draw(Canvas(image))
        }
        // Recolor the mark's teal silhouette and white cutout separately so its
        // shape stays recognizable, rather than tinting it into a solid disk.
        for (y in 0 until 64) for (x in 0 until 64) {
            val pixel = image.getPixel(x, y)
            if (Color.alpha(pixel) == 0) continue
            val amount = ((255 - Color.red(pixel)) / 144f).coerceIn(0f, 1f)
            image.setPixel(x, y, ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(background, accent, amount), Color.alpha(pixel)))
        }
        return image
    }

    companion object {
        fun from(prefs: WidgetPreferences): WidgetPalette = when (prefs.theme) {
            "light" -> WidgetPalette(0xFFF1FBFA.toInt(), Color.rgb(12,43,55), Color.rgb(9,86,91), Color.rgb(47,77,85), false)
            "xray" -> WidgetPalette(Color.BLACK, Color.WHITE, Color.WHITE, 0xFFD0D0D0.toInt(), true)
            "custom" -> prefs.customColors.normalized().let {
                WidgetPalette(it.background, it.text, it.accent,
                    ColorUtils.blendARGB(it.background, it.text, .85f), true)
            }
            else -> WidgetPalette(0xFF050C18.toInt(), Color.rgb(222,255,249), Color.rgb(120,255,224), Color.rgb(181,218,219), false)
        }
    }
}
