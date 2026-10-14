package com.kaspa.livewidget.widget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.widget.*
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
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
class WidgetControlsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun idle() = Shadows.shadowOf(Looper.getMainLooper()).idle()
    @Before fun isolateWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setTaskExecutor(SynchronousExecutor()).setExecutor(Executor { }).build(),
            WorkManagerTestInitHelper.ExecutorsMode.PRESERVE_EXECUTORS)
    }

    @Test fun oldPaletteInheritsAccentAndOverridesPersistAcrossRestart() {
        val prefs = context.getSharedPreferences("kaspa_widget_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("theme", "custom").putInt("color_accent", Color.MAGENTA).commit()
        val settings = WidgetSettings(context)
        val old = settings.get(); val palette = WidgetPalette.from(old)
        assertEquals(50, old.chartIntensity)
        assertEquals(Color.MAGENTA, palette.clock); assertEquals(Color.MAGENTA, palette.price)
        val colors = old.customColors.copy(clock = Color.RED, price = Color.GREEN, chart = Color.BLUE, network = Color.YELLOW)
        settings.save(old.copy(customColors = colors, chartIntensity = 23))
        assertEquals(colors, WidgetSettings(context).get().customColors)
        assertEquals(23, WidgetSettings(context).get().chartIntensity)
        for (large in listOf(false, true)) {
            val view = WidgetRenderer.views(context, large).apply(context, FrameLayout(context))
            assertEquals(Color.RED, view.findViewById<TextView>(R.id.widget_time).currentTextColor)
            assertEquals(Color.GREEN, view.findViewById<TextView>(R.id.widget_price).currentTextColor)
            assertEquals(Color.YELLOW, view.findViewById<TextView>(R.id.widget_block_height).currentTextColor)
            if (large) assertEquals(Color.YELLOW, view.findViewById<TextView>(R.id.widget_bps).currentTextColor)
        }
        assertEquals(Color.BLUE, WidgetPalette.from(settings.get()).chart)
        settings.save(old.copy(chartIntensity = 500)); assertEquals(100, settings.get().chartIntensity)
    }

    @Test fun chartZeroHidesRealLineAndIntensityChangesItsPixelsWithoutInventingData() {
        val points = listOf(PricePoint(1000, .03), PricePoint(2000, .04))
        val grid = PriceChart.glass(emptyList(), false, 280, 360, intensity = 0)
        assertTrue(grid.sameAs(PriceChart.glass(points, false, 280, 360, intensity = 0)))
        val subtle = PriceChart.glass(points, false, 280, 360, intensity = 20)
        val bright = PriceChart.glass(points, false, 280, 360, intensity = 100)
        assertFalse(subtle.sameAs(bright)); assertFalse(subtle.sameAs(grid))
        assertTrue(grid.sameAs(PriceChart.glass(listOf(points.first()), false, 280, 360, intensity = 100)))
    }

    @Test fun presetsAreBoundedReplaceByNameAndNeverChangeCurrencyOrRefresh() {
        val store = WidgetPresets(context)
        val look = WidgetPreferences(theme = "custom", customColors = WidgetColors(chart = Color.BLUE), chartIntensity = 12)
        assertFalse(store.save(" ", look))
        assertTrue(store.save("Night", look))
        assertTrue(WidgetPresets(context).save("NIGHT", look.copy(chartIntensity = 30)))
        assertEquals(1, store.all().size)
        val applied = store.all().single().applyTo(WidgetPreferences("GBP", 60, "dark", false))
        assertEquals("GBP", applied.currency); assertEquals(60L, applied.intervalMinutes); assertFalse(applied.showChart)
        assertEquals(30, applied.chartIntensity); assertEquals(Color.BLUE, applied.customColors.chart)
        for (i in 1..19) assertTrue(store.save("Look $i", look))
        assertFalse(store.save("Overflow", look)); store.delete("NIGHT"); assertEquals(19, store.all().size)
        context.getSharedPreferences("kaspa_widget_presets", Context.MODE_PRIVATE).edit().putString("styles", "broken").commit()
        assertTrue(store.all().isEmpty())
    }

    @Test fun separateColorAndPresetDraftSurviveRotationWithoutApplyingSettings() {
        val saved = WidgetPreferences("GBP", 60)
        WidgetSettings(context).save(saved)
        val state = Bundle()
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val app = controller.get()
            app.findViewById<Spinner>(R.id.appearance_choice).setSelection(3); idle()
            app.findViewById<Button>(R.id.color_clock).performClick()
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            dialog.findViewById<EditText>(R.id.color_hex).setText("FF0000")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            app.findViewById<SeekBar>(R.id.chart_intensity).progress = 17; idle()
            app.findViewById<Button>(R.id.save_preset).performClick()
            val name = ShadowAlertDialog.getLatestAlertDialog()
            name.findViewById<EditText>(R.id.preset_name).setText("My red clock")
            name.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertEquals(saved, WidgetSettings(context).get())
            assertEquals(Color.RED, WidgetPresets(context).all().single().colors.clock)
            controller.saveInstanceState(state)
        }
        Robolectric.buildActivity(SettingsActivity::class.java).create(state).start().restoreInstanceState(state).resume().visible().use { controller ->
            val app = controller.get(); idle()
            assertEquals(17, app.findViewById<SeekBar>(R.id.chart_intensity).progress)
            assertTrue(app.findViewById<Button>(R.id.color_clock).text.contains("#FF0000"))
            app.findViewById<Button>(R.id.apply_settings).performClick()
            assertEquals(Color.RED, WidgetSettings(context).get().customColors.clock)
            assertEquals(17, WidgetSettings(context).get().chartIntensity)
            assertEquals("GBP", WidgetSettings(context).get().currency)
        }
    }

    @Test fun feedbackNeverCallsOfflineExpiredOrStaleReadingsFresh() {
        val now = 4_000_000L
        val market = MarketData(price = .03, timestamp = now - 1000)
        val network = KaspaNetworkData(blockHeight = 123, networkTimestamp = now - 1000)
        val complete = RefreshStatus("complete", now - 1000)
        assertEquals("Offline · tap to retry", RefreshFeedback.caption(complete, market, network, true, now))
        assertEquals("Price stale · tap to retry", RefreshFeedback.caption(complete, market.copy(timestamp = 1000), network, false, now))
        assertEquals("Network stale · retry", RefreshFeedback.caption(complete, market, network.copy(networkTimestamp = 1000), false, now))
        assertEquals("Tap to retry", RefreshFeedback.caption(RefreshStatus("running", 1000), market, network, false, now))
        assertEquals("Some feeds delayed", RefreshFeedback.caption(RefreshStatus("partial", now - 1), market, network, false, now))
        assertEquals("Refreshing…", RefreshFeedback.caption(RefreshStatus("running", now - 1), market, network, false, now))
        assertEquals("Waiting for price", RefreshFeedback.caption(complete, MarketData(), network, false, now))
        assertTrue(RefreshFeedback.ages(market, network, now).contains("less than a minute"))
    }
}
