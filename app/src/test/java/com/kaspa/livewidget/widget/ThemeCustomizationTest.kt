package com.kaspa.livewidget.widget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.kaspa.livewidget.ColorEditor
import com.kaspa.livewidget.R
import com.kaspa.livewidget.SettingsActivity
import com.kaspa.livewidget.data.*
import com.kaspa.livewidget.utils.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34, 36], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeCustomizationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun idle() = Shadows.shadowOf(Looper.getMainLooper()).idle()
    @Before fun isolateWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setTaskExecutor(SynchronousExecutor()).setExecutor(Executor { }).build(),
            WorkManagerTestInitHelper.ExecutorsMode.PRESERVE_EXECUTORS)
    }

    @Test fun legacySettingsAndSavedCustomColorsSurviveThemeSwitches() {
        context.getSharedPreferences("kaspa_widget_settings", Context.MODE_PRIVATE).edit()
            .putString("currency", "GBP").putLong("interval", 30).putString("theme", "light").putBoolean("chart", false).commit()
        val settings = WidgetSettings(context)
        assertEquals(WidgetPreferences("GBP", 30, "light", false), settings.get())
        val colors = WidgetColors(0x00112233, 0x00EEEEEE, 0x00FF00A8)
        settings.save(settings.get().copy(theme = "custom", customColors = colors))
        assertEquals(colors.normalized(), settings.get().customColors)
        settings.save(settings.get().copy(theme = "xray"))
        assertEquals("xray", settings.get().theme)
        assertEquals(colors.normalized(), settings.get().customColors)
        settings.save(settings.get().copy(theme = "custom"))
        assertEquals(colors.normalized(), settings.get().customColors)
    }

    @Test fun editorValidatesCancelsPreviewsRotatesAndSavesExplicitly() {
        val saved = WidgetPreferences()
        WidgetSettings(context).save(saved)
        val state = Bundle()
        val chosen = Color.rgb(255, 0, 168)
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val app = controller.get()
            app.findViewById<Spinner>(R.id.appearance_choice).setSelection(3); idle()
            assertEquals(View.VISIBLE, app.findViewById<View>(R.id.color_controls).visibility)
            app.findViewById<Button>(R.id.color_accent).performClick()
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            val input = dialog.findViewById<EditText>(R.id.color_hex)
            input.setText("#NOPE")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertTrue(dialog.isShowing); assertNotNull(input.error)
            assertEquals(saved, WidgetSettings(context).get())
            input.setText("#FF00A8")
            assertEquals(255, dialog.findViewById<SeekBar>(R.id.color_red).progress)
            assertEquals(0, dialog.findViewById<SeekBar>(R.id.color_green).progress)
            assertEquals(168, dialog.findViewById<SeekBar>(R.id.color_blue).progress)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); idle()
            val preview = app.findViewById<FrameLayout>(R.id.widget_preview).getChildAt(0)
            assertEquals(chosen, preview.findViewById<TextView>(R.id.widget_price).currentTextColor)
            assertEquals(saved, WidgetSettings(context).get())
            assertEquals("idle", RefreshStatusCache(context).get().phase)
            app.findViewById<Button>(R.id.color_background).performClick()
            val canceled = ShadowAlertDialog.getLatestAlertDialog()
            canceled.findViewById<EditText>(R.id.color_hex).setText("#FF0000")
            canceled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            controller.saveInstanceState(state)
        }
        Robolectric.buildActivity(SettingsActivity::class.java).create(state).start().restoreInstanceState(state).resume().visible().use { controller ->
            val app = controller.get(); idle()
            assertEquals("Custom colors", app.findViewById<Spinner>(R.id.appearance_choice).selectedItem)
            assertTrue(app.findViewById<Button>(R.id.color_accent).text.contains("#FF00A8"))
            assertTrue(app.findViewById<Button>(R.id.color_background).text.contains("#050C18"))
            assertEquals(saved, WidgetSettings(context).get())
            app.findViewById<Button>(R.id.apply_settings).performClick()
            assertEquals(saved.copy(theme = "custom", customColors = WidgetColors(accent = chosen)), WidgetSettings(context).get())
            app.findViewById<Button>(R.id.reset_colors).performClick()
            assertEquals(chosen, WidgetSettings(context).get().customColors.accent)
            app.findViewById<Button>(R.id.apply_settings).performClick()
            assertEquals(WidgetColors(), WidgetSettings(context).get().customColors)
        }
        assertNull(ColorEditor.parse("#1234")); assertNull(ColorEditor.parse("transparent"))
        assertEquals(Color.WHITE, ColorEditor.parse("ffffff"))
    }

    @Test fun xrayIsGrayscaleAndCustomPaletteReachesBothActualWidgetSizes() {
        val now = System.currentTimeMillis()
        MarketCache(context).save(MarketData(price = .03, change24h = -2.0, timestamp = now, changeTimestamp = now,
            history = listOf(PricePoint(now - 1000, .02), PricePoint(now, .03)), historyTimestamp = now))
        val custom = WidgetColors(0xFF160A28.toInt(), 0xFFFFF0D0.toInt(), 0xFFFF00A8.toInt())
        for (theme in listOf("xray", "custom")) for (large in listOf(false, true)) {
            val prefs = WidgetPreferences(theme = theme, customColors = custom)
            val palette = WidgetPalette.from(prefs)
            val widget = WidgetRenderer.views(context, large, previewPreferences = prefs).apply(context, FrameLayout(context))
            assertEquals(palette.accent, widget.findViewById<TextView>(R.id.widget_price).currentTextColor)
            assertEquals(palette.text, widget.findViewById<TextView>(R.id.widget_change).currentTextColor)
            assertEquals(palette.text, widget.findViewById<TextView>(R.id.widget_block_height).currentTextColor)
            if (large) assertEquals(palette.text, widget.findViewById<TextView>(R.id.widget_bps).currentTextColor)
            val width = if (large) 500 else 280; val height = if (large) 600 else 360
            widget.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            widget.layout(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            widget.draw(Canvas(bitmap))
            if (theme == "xray") {
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                val colored = pixels.indexOfFirst {
                    val red = (it ushr 16) and 255
                    red != ((it ushr 8) and 255) || red != (it and 255)
                }
                assertEquals("Colored pixel in X-ray theme at index $colored", -1, colored)
            } else {
                val pixel = bitmap.getPixel(width / 2, height - 20)
                assertTrue("Custom background must retain its purple hue", Color.blue(pixel) > Color.green(pixel))
            }
        }
    }
}
