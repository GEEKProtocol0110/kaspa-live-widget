package com.kaspa.livewidget.widget

import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextClock
import android.widget.TextView
import com.kaspa.livewidget.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** Inflate through RemoteViews, as a launcher does, rather than an Activity inflater. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetLayoutTest {
    @Test fun bothSizesHaveHostDrivenClocksAndReadableRefreshHint() {
        val context = RuntimeEnvironment.getApplication()
        Robolectric.buildActivity(Activity::class.java).setup().use { host ->
            for (layout in listOf(R.layout.widget_layout, R.layout.widget_layout_large)) {
                Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
                val widget = RemoteViews(context.packageName, layout).apply(host.get(), FrameLayout(host.get()))
                host.get().setContentView(widget)
                val clock = widget.findViewById<TextClock>(R.id.widget_time)
                assertNotNull(clock)
                // Robolectric does not aggregate launcher visibility automatically.
                clock.onVisibilityAggregated(true)
                assertTrue("clock=${clock.text}, visibility=${clock.visibility}, attached=${clock.isAttachedToWindow}", clock.text.isNotBlank())
                assertEquals(context.getString(R.string.refresh_hint), widget.contentDescription)
                assertTrue(clock.is24HourModeEnabled)
                val beforeTick = clock.text.toString()
                ShadowSystemClock.advanceBy(Duration.ofMinutes(1))
                context.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
                shadowOf(Looper.getMainLooper()).idle()
                assertNotEquals(beforeTick, clock.text.toString())
                Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "12")
                context.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
                shadowOf(Looper.getMainLooper()).idle()
                assertFalse(clock.is24HourModeEnabled)
            }
        }
    }

    @Test fun readingsAndStaleMessagesFitTheDeclaredMinimumSizes() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density
        for ((layout, width, height) in listOf(
            Triple(R.layout.widget_layout, 140, 180),
            Triple(R.layout.widget_layout_large, 250, 300)
        )) {
            val views = RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.widget_price, "$0.0312")
                setTextViewText(R.id.widget_block_height, "362,815,907")
                setTextViewText(R.id.widget_updated, "Network stale · Stale since 06:15")
                if (layout == R.layout.widget_layout_large) {
                    setTextViewText(R.id.widget_bps, "10.00")
                    setTextViewText(R.id.widget_hashrate, "427.91 PH/s")
                    setTextViewText(R.id.widget_hashrate_updated, "Stale since 06:15")
                }
            }
            val widget = views.apply(context, FrameLayout(context)) as ViewGroup
            val pixelsWide = (width * density).toInt()
            val pixelsHigh = (height * density).toInt()
            widget.measure(
                View.MeasureSpec.makeMeasureSpec(pixelsWide, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(pixelsHigh, View.MeasureSpec.EXACTLY)
            )
            widget.layout(0, 0, pixelsWide, pixelsHigh)
            fun checkText(parent: ViewGroup) {
                for (index in 0 until parent.childCount) {
                    val child = parent.getChildAt(index)
                    if (child is TextView && child.text.isNotBlank()) {
                        val bounds = Rect(0, 0, child.width, child.height)
                        widget.offsetDescendantRectToMyCoords(child, bounds)
                        assertTrue("${child.text} exceeds widget height: $bounds / $pixelsHigh; layout=$layout", bounds.bottom <= pixelsHigh)
                        assertTrue("${child.text} exceeds widget width", bounds.right <= pixelsWide)
                        val textLayout = child.layout
                        assertTrue("${child.text} is clipped vertically",
                            textLayout.height <= child.height - child.compoundPaddingTop - child.compoundPaddingBottom)
                        for (line in 0 until textLayout.lineCount) {
                            assertTrue("${child.text} is clipped horizontally",
                                textLayout.getLineWidth(line) <= child.width - child.compoundPaddingLeft - child.compoundPaddingRight + 1)
                        }
                    } else if (child is ViewGroup) checkText(child)
                }
            }
            checkText(widget)
        }
    }
}
