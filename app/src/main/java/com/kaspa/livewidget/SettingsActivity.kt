package com.kaspa.livewidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import com.kaspa.livewidget.utils.*
import com.kaspa.livewidget.widget.*

/** Settings apply to both sizes and all installed widget instances. */
class SettingsActivity : Activity() {
    private lateinit var currency: Spinner
    private lateinit var interval: Spinner
    private lateinit var appearance: Spinner
    private lateinit var chart: Switch
    private lateinit var reading: TextView
    private lateinit var historyLabel: TextView
    private lateinit var preview: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private val update = object : Runnable {
        override fun run() { display(); handler.postDelayed(this, 2000L) }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val density = resources.displayMetrics.density
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((20*density).toInt(), (20*density).toInt(), (20*density).toInt(), (20*density).toInt()) }
        val scroll = ScrollView(this).apply { addView(body) }; setContentView(scroll)
        fun label(text: String): TextView = TextView(this).apply { this.text = text; textSize = 16f; setPadding(0, 12, 0, 12); body.addView(this) }
        fun button(text: String, id: Int, action: () -> Unit) { body.addView(Button(this).apply { this.text = text; this.id = id; setOnClickListener { action() } }) }
        fun select(title: String, entries: List<String>, id: Int): Spinner {
            label(title)
            return Spinner(this).apply {
                this.id = id
                adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, entries)
                body.addView(this)
            }
        }
        label("Kaspa Live Widget").textSize = 26f
        label("Add a widget to your home screen. Tap it to refresh; tap Setup to return here.")
        button("Add compact widget", R.id.add_compact) { pin(false) }
        button("Add expanded widget", R.id.add_expanded) { pin(true) }
        label("If your launcher cannot add widgets here: long-press an empty home-screen space → Widgets → Kaspa Live Widget. Drag a widget onto the screen.")
        currency = select("Price currency", WidgetPreferences.CURRENCIES, R.id.currency_choice)
        interval = select("Background refresh", WidgetPreferences.INTERVALS.map { "$it minutes" }, R.id.interval_choice)
        appearance = select("Appearance", listOf("Neon dark", "Frosted light"), R.id.appearance_choice)
        chart = Switch(this).apply { text = "Show neon 24-hour chart behind glass"; id = R.id.chart_choice; body.addView(this) }
        val prefs = WidgetSettings(this).get()
        currency.setSelection(WidgetPreferences.CURRENCIES.indexOf(prefs.currency))
        interval.setSelection(WidgetPreferences.INTERVALS.indexOf(prefs.intervalMinutes))
        appearance.setSelection(WidgetPreferences.THEMES.indexOf(prefs.theme))
        chart.isChecked = prefs.showChart
        label("Settings apply to all your Kaspa widgets. Android may delay background updates. The real price chart glows behind the glass on both widget sizes. Missing and stale history stay labelled.")
        button("Apply settings", R.id.apply_settings) {
            WidgetSettings(this).save(WidgetPreferences(WidgetPreferences.CURRENCIES[currency.selectedItemPosition],
                WidgetPreferences.INTERVALS[interval.selectedItemPosition], WidgetPreferences.THEMES[appearance.selectedItemPosition], chart.isChecked))
            WidgetRenderer.updateAll(this)
            if (WorkScheduler.hasWidgets(this)) WorkScheduler.scheduleWidgetUpdates(this, true)
            else WorkScheduler.refreshNow(this, true)
            display()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }
        button("Refresh now", R.id.refresh_now) { WorkScheduler.refreshNow(this); display() }
        reading = label("")
        historyLabel = label("")
        preview = ImageView(this).apply { body.addView(this, LinearLayout.LayoutParams(-1, (100*density).toInt())) }
        label("Prices: CoinGecko; USD fallback uses KAS/USDT as an approximation. Network: Kaspa REST with a validated mainnet node fallback. Missing feeds keep their last reading and time.")
        button("Downloads and release notes", R.id.release_notes) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GEEKProtocol0110/kaspa-live-widget/releases")))
        }
    }
    override fun onResume() { super.onResume(); handler.post(update) }
    override fun onPause() { handler.removeCallbacks(update); super.onPause() }
    private fun pin(large: Boolean) {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            val component = ComponentName(this, if (large) KaspaWidgetLargeProvider::class.java else KaspaWidgetProvider::class.java)
            if (!manager.requestPinAppWidget(component, null, null)) Toast.makeText(this, "Use your home screen’s Widgets menu", Toast.LENGTH_LONG).show()
        } else Toast.makeText(this, "Long-press your home screen → Widgets → Kaspa Live Widget", Toast.LENGTH_LONG).show()
    }
    private fun display() {
        val settings = WidgetSettings(this).get()
        val market = MarketCache(this).get(settings.currency)
        reading.text = "${RefreshStatusCache(this).get().label(settings.currency)}\n${MarketDisplay.price(market)} · ${MarketDisplay.change(market)}\n${WidgetFreshness.label(market.timestamp)}\n${market.source}"
        historyLabel.text = if (settings.showChart) MarketDisplay.historyLabel(market) else "Price chart is off"
        preview.visibility = if (settings.showChart && market.history.size >= 2) View.VISIBLE else View.GONE
        if (preview.visibility == View.VISIBLE) {
            preview.setImageBitmap(PriceChart.bitmap(market.history, android.graphics.Color.rgb(0, 120, 100)))
            preview.contentDescription = MarketDisplay.historyLabel(market)
        }
    }
}
