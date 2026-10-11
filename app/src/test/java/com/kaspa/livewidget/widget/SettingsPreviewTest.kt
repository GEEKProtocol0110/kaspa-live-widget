package com.kaspa.livewidget.widget

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.*
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34, 36], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPreviewTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun idle() = Shadows.shadowOf(Looper.getMainLooper()).idle()
    @Before fun isolateBackgroundWork() {
        // Exercise scheduling and UI transitions without running a real network worker.
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setTaskExecutor(SynchronousExecutor()).setExecutor(Executor { }).build(),
            WorkManagerTestInitHelper.ExecutorsMode.PRESERVE_EXECUTORS)
    }

    @Test fun systemInsetsDoNotClipControlsOrAccumulateOnRepeatedDispatch() {
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val root = controller.get().findViewById<View>(R.id.setup_body).parent as View
            val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(7, 31, 9, 42)).build()
            repeat(2) { ViewCompat.dispatchApplyWindowInsets(root, insets) }
            assertEquals(7, root.paddingLeft); assertEquals(31, root.paddingTop)
            assertEquals(9, root.paddingRight); assertEquals(42, root.paddingBottom)
            assertNotNull(controller.get().findViewById<Button>(R.id.privacy_policy))
        }
    }

    @Test fun draftPreviewStaysIsolatedSurvivesRotationAndSavesExplicitly() {
        val saved = WidgetPreferences()
        WidgetSettings(context).save(saved)
        MarketCache(context).save(MarketData(currency = "EUR", price = .029, timestamp = System.currentTimeMillis()))
        val state = Bundle()
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val app = controller.get()
            app.findViewById<Spinner>(R.id.currency_choice).setSelection(1)
            app.findViewById<Spinner>(R.id.appearance_choice).setSelection(1)
            app.findViewById<Spinner>(R.id.interval_choice).setSelection(2)
            app.findViewById<Spinner>(R.id.preview_choice).setSelection(1)
            app.findViewById<Switch>(R.id.chart_choice).isChecked = false
            idle()
            assertEquals(saved, WidgetSettings(context).get())
            assertEquals("idle", RefreshStatusCache(context).get().phase)
            val preview = app.findViewById<FrameLayout>(R.id.widget_preview).getChildAt(0)
            assertEquals("EUR 0.0290", preview.findViewById<TextView>(R.id.widget_price).text.toString())
            assertEquals(View.GONE, preview.findViewById<View>(R.id.widget_chart_group).visibility)
            assertFalse(preview.isClickable)
            assertFalse(preview.findViewById<View>(R.id.widget_settings).isClickable)
            assertTrue(app.findViewById<Button>(R.id.apply_settings).isEnabled)
            controller.saveInstanceState(state)
        }
        Robolectric.buildActivity(SettingsActivity::class.java).create(state).start().restoreInstanceState(state).resume().visible().use { controller ->
            val app = controller.get(); idle()
            assertEquals("EUR", app.findViewById<Spinner>(R.id.currency_choice).selectedItem)
            assertEquals("Frosted light", app.findViewById<Spinner>(R.id.appearance_choice).selectedItem)
            assertEquals("60 minutes", app.findViewById<Spinner>(R.id.interval_choice).selectedItem)
            assertEquals(1, app.findViewById<Spinner>(R.id.preview_choice).selectedItemPosition)
            assertFalse(app.findViewById<Switch>(R.id.chart_choice).isChecked)
            assertEquals(saved, WidgetSettings(context).get())
            app.findViewById<Button>(R.id.apply_settings).performClick()
            assertEquals(WidgetPreferences("EUR", 60, "light", false), WidgetSettings(context).get())
            assertFalse(app.findViewById<Button>(R.id.apply_settings).isEnabled)
            assertEquals("EUR", RefreshStatusCache(context).get().currency)
        }
    }

    @Test fun refreshProgressPreventsAnotherTapAndIdlePreviewIsReused() {
        RefreshStatusCache(context).set("running", "USD")
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val app = controller.get(); idle()
            val refresh = app.findViewById<Button>(R.id.refresh_now)
            val frame = app.findViewById<FrameLayout>(R.id.widget_preview)
            val first = frame.getChildAt(0)
            assertFalse(refresh.isEnabled)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            assertSame(first, frame.getChildAt(0))
            RefreshStatusCache(context).set("complete", "USD")
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            assertTrue(refresh.isEnabled)
            refresh.performClick()
            assertFalse(refresh.isEnabled)
            assertEquals("queued", RefreshStatusCache(context).get().phase)
        }
    }

    @Test fun saveAndAddKeepsMissingCurrencyHonestAndOffersLauncherHelp() {
        MarketCache(context).save(MarketData(price = .0312, timestamp = System.currentTimeMillis()))
        Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
            val app = controller.get()
            app.findViewById<Spinner>(R.id.currency_choice).setSelection(2); idle()
            val preview = app.findViewById<FrameLayout>(R.id.widget_preview).getChildAt(0)
            assertEquals("GBP --", preview.findViewById<TextView>(R.id.widget_price).text.toString())
            assertTrue(app.findViewById<TextView>(R.id.preview_note).text.contains("No saved GBP"))
            app.findViewById<Button>(R.id.add_compact).performClick()
            assertEquals("GBP", WidgetSettings(context).get().currency)
            assertEquals(.0312, MarketCache(context).get("USD").price, .000001)
            assertTrue(ShadowToast.getTextOfLatestToast().contains("Widgets"))
        }
    }

    @Test fun bothSetupThemesRenderWithSavedReadings() {
        val now = System.currentTimeMillis()
        DataCache(context).saveData(KaspaNetworkData(blockHeight = 561810469, bps = 10.0, hashrate = "374.58 PH/s",
            networkTimestamp = now, hashrateTimestamp = now, timestamp = now))
        MarketCache(context).save(MarketData(price = .0312, change24h = -2.3, timestamp = now, changeTimestamp = now,
            history = (0..48).map { PricePoint(now - 24 * 3600_000L + it * 1800_000L, .029 + kotlin.math.sin(it * .5) * .0007) }, historyTimestamp = now))
        RefreshStatusCache(context).set("complete", "USD")
        for (theme in listOf("dark", "light")) {
            WidgetSettings(context).save(WidgetPreferences(theme = theme))
            Robolectric.buildActivity(SettingsActivity::class.java).setup().use { controller ->
                val app = controller.get(); idle()
                val root = app.findViewById<View>(android.R.id.content)
                val width = 720; val height = 1600
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                app.findViewById<TextClock>(R.id.widget_time).onVisibilityAggregated(true)
                assertEquals("USD 0.0312", app.findViewById<TextView>(R.id.widget_price).text.toString())
                assertFalse(app.findViewById<Button>(R.id.apply_settings).isEnabled)
                val output = System.getProperty("widget.preview.dir")
                if (output != null && android.os.Build.VERSION.SDK_INT == 34) {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    root.draw(Canvas(bitmap)); File(output).mkdirs()
                    FileOutputStream(File(output, "setup-$theme.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }
}
