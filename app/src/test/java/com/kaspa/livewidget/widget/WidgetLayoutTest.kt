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
import android.graphics.Bitmap
import android.graphics.Canvas
import com.kaspa.livewidget.data.*
import com.kaspa.livewidget.utils.*
import java.io.File
import java.io.FileOutputStream
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
@Config(sdk = [26, 34, 36])
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

    @Test @Config(qualifiers="xhdpi") fun rendererAppliesCurrencyThemeFeedbackAndResponsiveChart() {
        val context = RuntimeEnvironment.getApplication()
        val now = System.currentTimeMillis()
        DataCache(context).saveData(KaspaNetworkData(blockHeight=561810469,blockCount=1210217,bps=10.0,
            hashrate="374.58 PH/s",networkTimestamp=now,hashrateTimestamp=now,timestamp=now))
        MarketCache(context).save(MarketData(currency="USD",price=.0312,change24h=-2.3,timestamp=now,changeTimestamp=now,
            history=(0..48).map { step ->
                PricePoint(now-24*3600_000L+step*1800_000L,
                    .028+step*.00003+kotlin.math.sin(step*.5)*.0007+kotlin.math.cos(step*.17)*.0004)
            },historyTimestamp=now))
        for (theme in listOf("dark","light")) {
            WidgetSettings(context).save(WidgetPreferences("USD",30,theme,true))
            RefreshStatusCache(context).set("running","USD",now)
            for ((large,height) in listOf(false to 180,false to 300,false to 420,true to 300,true to 380)) {
                val widget = WidgetRenderer.views(context,large,height).apply(context,FrameLayout(context))
                assertEquals("USD 0.0312",widget.findViewById<TextView>(R.id.widget_price).text.toString())
                assertEquals("24h -2.30%",widget.findViewById<TextView>(R.id.widget_change).text.toString())
                assertEquals(View.VISIBLE,widget.findViewById<View>(R.id.widget_progress).visibility)
                assertEquals(View.VISIBLE,widget.findViewById<View>(R.id.widget_chart_group).visibility)
                val density = context.resources.displayMetrics.density
                val width = ((if (large) 250 else 140)*density).toInt()
                val pixelsHigh = (height*density).toInt()
                widget.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(pixelsHigh,View.MeasureSpec.EXACTLY))
                widget.layout(0,0,width,pixelsHigh)
                checkBounds(widget as ViewGroup,pixelsHigh,width)
                val caption = widget.findViewById<TextView>(R.id.widget_chart_label)
                assertEquals("24h USD chart", caption.text.toString())
                assertEquals("chart caption must fit compact widths", 0, caption.layout.getEllipsisCount(0))
                if (height >= 300) {
                    val priceBounds=Rect()
                    val scoreBounds=Rect()
                    widget.findViewById<View>(R.id.price_panel).getDrawingRect(priceBounds)
                    widget.offsetDescendantRectToMyCoords(widget.findViewById(R.id.price_panel),priceBounds)
                    widget.findViewById<View>(R.id.network_panel).getDrawingRect(scoreBounds)
                    widget.offsetDescendantRectToMyCoords(widget.findViewById(R.id.network_panel),scoreBounds)
                    assertTrue("price must overlay chart in tall widgets: $priceBounds",priceBounds.centerY()<pixelsHigh*.72)
                    assertTrue("score must overlay chart: $scoreBounds",scoreBounds.centerY()<pixelsHigh*.88)
                }
                val output = System.getProperty("widget.preview.dir")
                if (output != null && android.os.Build.VERSION.SDK_INT == 34) {
                    Robolectric.buildActivity(Activity::class.java).setup().use { host ->
                        host.get().setContentView(widget)
                        widget.findViewById<TextClock>(R.id.widget_time).onVisibilityAggregated(true)
                        widget.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(pixelsHigh,View.MeasureSpec.EXACTLY))
                        widget.layout(0,0,width,pixelsHigh)
                    checkBounds(widget as ViewGroup,pixelsHigh,width)
                    val bitmap = Bitmap.createBitmap(width,pixelsHigh,Bitmap.Config.ARGB_8888)
                    widget.draw(Canvas(bitmap))
                    File(output).mkdirs()
                    FileOutputStream(File(output,"widget-$theme-${if (large) "large" else "compact"}-$height.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                    }
                }
            }
        }
    }

    private fun checkBounds(widget: ViewGroup, height: Int, width: Int) {
        fun walk(parent: ViewGroup) {
            for(index in 0 until parent.childCount) {
                val child=parent.getChildAt(index)
                if(child.visibility==View.GONE) continue
                if(child is TextView && child.text.isNotBlank()) {
                    val bounds=Rect(0,0,child.width,child.height)
                    widget.offsetDescendantRectToMyCoords(child,bounds)
                    assertTrue("${child.text} exceeds height: $bounds / $height",bounds.bottom<=height)
                    assertTrue("${child.text} exceeds width: $bounds / $width",bounds.right<=width)
                    assertTrue("${child.text} clipped vertically",child.layout.height<=child.height-child.compoundPaddingTop-child.compoundPaddingBottom)
                } else if(child is ViewGroup) walk(child)
            }
        }
        walk(widget)
    }

    @Test fun chartToggleKeepsGlassWithoutInventingMarketHistory() {
        val context=RuntimeEnvironment.getApplication()
        RefreshStatusCache(context).set("partial", "USD")
        for (enabled in listOf(true,false)) {
            WidgetSettings(context).save(WidgetPreferences(showChart=enabled))
            MarketCache(context).save(MarketData())
            for (large in listOf(false,true)) {
                val widget=WidgetRenderer.views(context,large).apply(context,FrameLayout(context))
                assertNotNull(widget.findViewById<android.widget.ImageView>(R.id.widget_logo).drawable)
                assertNotNull(widget.findViewById<android.widget.ImageView>(R.id.widget_chart).drawable)
                val status = widget.findViewById<TextView>(R.id.widget_status)
                assertEquals("Some feeds delayed", status.text.toString())
                assertEquals("Some feeds delayed · cached readings kept", status.contentDescription)
                assertEquals(if(enabled) View.VISIBLE else View.GONE,widget.findViewById<View>(R.id.widget_chart_group).visibility)
                if(enabled) assertEquals("24h chart unavailable",widget.findViewById<TextView>(R.id.widget_chart_label).text.toString())
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
                setTextViewText(R.id.widget_status, "Update unavailable · cache kept")
                setTextViewText(R.id.widget_change, "24h +3.21% · stale")
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
                    if (child.visibility == View.GONE) continue
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
