package com.kaspa.livewidget

import android.app.Activity
import android.app.AlertDialog
import android.text.InputFilter
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.ColorUtils
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.data.ChartRange
import com.kaspa.livewidget.update.AppUpdateActivity
import com.kaspa.livewidget.update.UpdateStore
import com.kaspa.livewidget.update.AppUpdates
import com.kaspa.livewidget.utils.*
import com.kaspa.livewidget.widget.*

/** Draft previews never write global settings or fetch another currency's feed. */
class SettingsActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var currency: Spinner
    private lateinit var interval: Spinner
    private lateinit var chartRange: Spinner
    private lateinit var appearance: Spinner
    private lateinit var previewChoice: Spinner
    private lateinit var chart: Switch
    private lateinit var reading: TextView
    private lateinit var previewNote: TextView
    private lateinit var draftFeedback: TextView
    private lateinit var preview: FrameLayout
    private lateinit var applyButton: Button
    private lateinit var refreshButton: Button
    private lateinit var colorControls: LinearLayout
    private lateinit var colorNote: TextView
    private lateinit var backgroundColorButton: Button
    private lateinit var textColorButton: Button
    private lateinit var accentColorButton: Button
    private val detailColors = mutableMapOf<String, Button>()
    private lateinit var intensity: SeekBar
    private lateinit var intensityLabel: TextView
    private lateinit var presetNote: TextView
    private var draftColors = WidgetColors()
    private var activeTheme = "dark"
    private var ready = false
    private var previewState: List<Any?>? = null
    private val handler = Handler(Looper.getMainLooper())
    private val update = object : Runnable {
        override fun run() { display(); handler.postDelayed(this, 2000L) }
    }
    private val light get() = activeTheme == "light"
    private val xray get() = activeTheme == "xray"
    private val ink get() = if (xray) Color.WHITE else if (light) Color.rgb(12, 43, 55) else Color.rgb(222, 255, 249)
    private val muted get() = if (xray) Color.LTGRAY else if (light) Color.rgb(47, 77, 85) else Color.rgb(181, 218, 219)
    private val accent get() = if (xray) Color.WHITE else if (light) Color.rgb(9, 86, 91) else Color.rgb(120, 255, 224)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val saved = WidgetSettings(this).get()
        val draft = WidgetPreferences(state?.getString("draft_currency") ?: saved.currency,
            state?.takeIf { it.containsKey("draft_interval") }?.getLong("draft_interval") ?: saved.intervalMinutes,
            state?.getString("draft_theme") ?: saved.theme,
            state?.takeIf { it.containsKey("draft_chart") }?.getBoolean("draft_chart") ?: saved.showChart,
            WidgetColors(state?.getInt("draft_background", saved.customColors.background) ?: saved.customColors.background,
                state?.getInt("draft_text", saved.customColors.text) ?: saved.customColors.text,
                state?.getInt("draft_accent", saved.customColors.accent) ?: saved.customColors.accent,
                draftColor(state, "clock", saved.customColors.clock), draftColor(state, "price", saved.customColors.price),
                draftColor(state, "chart_color", saved.customColors.chart), draftColor(state, "network", saved.customColors.network)),
            state?.getInt("draft_intensity", saved.chartIntensity) ?: saved.chartIntensity,
            state?.getString("draft_chart_range") ?: saved.chartRange).normalized()
        draftColors = draft.customColors
        activeTheme = draft.theme
        body = LinearLayout(this).apply {
            id = R.id.setup_body; orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }
        scroll = ScrollView(this).apply { isFillViewport = true; addView(body) }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            // Use the insets themselves so repeated dispatches never accumulate padding.
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(scroll)
        val brand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(this).apply { id = R.id.setup_logo; setImageResource(R.drawable.kaspa_mark); contentDescription = "Kaspa" },
            LinearLayout.LayoutParams(dp(32), dp(32)))
        text("Kaspa Live Widget", brand, "title").apply { textSize = 22f; setPadding(dp(10), 0, 0, 0) }
        body.addView(brand)
        text("Your home-screen market and network display.", body, "secondary")

        val previewCard = section("YOUR WIDGET")
        previewChoice = select("Preview size", listOf("Compact · price & score", "Expanded · all readings"), R.id.preview_choice, previewCard)
        previewChoice.setSelection((state?.getInt("preview_size") ?: 0).coerceIn(0, 1))
        preview = FrameLayout(this).apply { id = R.id.widget_preview }
        preview.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) preview.post { display() }
        }
        previewCard.addView(preview)
        previewNote = text("", previewCard, "secondary").apply { id = R.id.preview_note }
        val adds = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        button("Save & add\ncompact", R.id.add_compact, adds) { save(); pin(false) }
            .layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginEnd = dp(4) }
        button("Save & add\nexpanded", R.id.add_expanded, adds) { save(); pin(true) }
            .layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginStart = dp(4) }
        previewCard.addView(adds)
        text("Adding a widget saves these choices. Settings apply to all your Kaspa widgets.", previewCard, "secondary")

        val preferences = section("MAKE IT YOURS")
        appearance = select("Appearance", listOf("Neon dark", "Frosted light", "X-ray · black & white", "Custom colors"), R.id.appearance_choice, preferences)
        colorControls = LinearLayout(this).apply { id = R.id.color_controls; orientation = LinearLayout.VERTICAL; preferences.addView(this) }
        text("Start with background, text and accent, then give the clock, price, chart or network readings their own color.", colorControls, "secondary")
        backgroundColorButton = button("", R.id.color_background, colorControls) {
            ColorEditor.show(this, "Background color", draftColors.background) { draftColors = draftColors.copy(background = it); display() }
        }
        textColorButton = button("", R.id.color_text, colorControls) {
            ColorEditor.show(this, "Text color", draftColors.text) { draftColors = draftColors.copy(text = it); display() }
        }
        accentColorButton = button("", R.id.color_accent, colorControls) {
            ColorEditor.show(this, "Accent color", draftColors.accent) { draftColors = draftColors.copy(accent = it); display() }
        }
        for ((key, title, id) in listOf(Triple("clock", "Clock", R.id.color_clock), Triple("price", "Price", R.id.color_price),
            Triple("chart", "Chart", R.id.color_chart), Triple("network", "Network stats", R.id.color_network))) {
            detailColors[key] = button(title, id, colorControls) {
                ColorEditor.show(this, "$title color", detailColor(key)) { color ->
                    draftColors = when (key) {
                        "clock" -> draftColors.copy(clock = color)
                        "price" -> draftColors.copy(price = color)
                        "chart" -> draftColors.copy(chart = color)
                        else -> draftColors.copy(network = color)
                    }
                    display()
                }
            }
        }
        button("Use accent/text for all readings", R.id.link_colors, colorControls) {
            draftColors = draftColors.copy(clock = null, price = null, chart = null, network = null); display()
        }
        button("Reset custom colors", R.id.reset_colors, colorControls) { draftColors = WidgetColors(); display() }
        colorNote = text("", colorControls, "secondary").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        val styles = section("SAVED LOOKS")
        presetNote = text("", styles, "secondary").apply { id = R.id.preset_note }
        button("Save this look", R.id.save_preset, styles) { savePreset() }
        button("Load a saved look", R.id.load_preset, styles) { choosePreset(false) }
        button("Delete a saved look", R.id.delete_preset, styles) { choosePreset(true) }
        text("Looks store appearance, colors and chart intensity on this device. Loading previews a draft; Apply settings updates your widgets.", styles, "secondary")
        currency = select("Price currency", WidgetPreferences.CURRENCIES, R.id.currency_choice, preferences)
        interval = select("Background refresh", WidgetPreferences.INTERVALS.map { "$it minutes" }, R.id.interval_choice, preferences)
        chart = Switch(this).apply {
            id = R.id.chart_choice; text = "Show price chart"; minHeight = dp(48); tag = "control"
            preferences.addView(this)
        }
        chartRange = select("Chart time range", ChartRange.values().map { it.label }, R.id.chart_range_choice, preferences)
        text("Chart range changes the price history shown. Background refresh controls how often new readings are requested.", preferences, "secondary")
        chartRange.setSelection(ChartRange.values().indexOf(ChartRange.fromKey(draft.chartRange)))
        currency.setSelection(WidgetPreferences.CURRENCIES.indexOf(draft.currency))
        interval.setSelection(WidgetPreferences.INTERVALS.indexOf(draft.intervalMinutes))
        appearance.setSelection(WidgetPreferences.THEMES.indexOf(draft.theme))
        chart.isChecked = draft.showChart
        intensityLabel = text("", preferences, "secondary").apply { id = R.id.intensity_label; labelFor = R.id.chart_intensity }
        intensity = SeekBar(this).apply {
            id = R.id.chart_intensity; max = 100; progress = draft.chartIntensity; contentDescription = "Chart intensity"
            preferences.addView(this, LinearLayout.LayoutParams(-1, dp(48)))
        }
        intensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar?, value: Int, fromUser: Boolean) { if (ready) display() }
            override fun onStartTrackingTouch(view: SeekBar?) = Unit
            override fun onStopTrackingTouch(view: SeekBar?) = Unit
        })
        draftFeedback = text("", preferences, "secondary").apply { id = R.id.draft_feedback; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        applyButton = button("Apply settings", R.id.apply_settings, preferences, true) { save() }

        val feeds = section("SAVED READINGS")
        reading = text("", feeds).apply { id = R.id.reading_status }
        refreshButton = button("Refresh saved settings", R.id.refresh_now, feeds) { WorkScheduler.refreshNow(this); display() }
        text("Refresh uses your saved currency. Android may delay background updates.", feeds, "secondary")
        val help = section("HELP & SOURCES")
        val details = text("Long-press an empty home-screen space → Widgets → Kaspa Live Widget. Drag either size onto your screen, then hold it to resize. Tap the widget to refresh, or Setup to return here.\n\nPrices: CoinGecko, then USD exchange fallbacks. KAS/USDT is an approximation of USD. Network: Kaspa REST with a validated mainnet node fallback. Missing feeds keep their last reading and time.", help, "secondary").apply {
            id = R.id.source_help; visibility = if (state?.getBoolean("help_open") == true) View.VISIBLE else View.GONE
        }
        button("Install help & data sources", R.id.toggle_help, help) {
            details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        button("App updates", R.id.app_updates, help) {
            startActivity(Intent(this, AppUpdateActivity::class.java))
        }
        button("Downloads & release notes", R.id.release_notes, help) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/GEEKProtocol0110/kaspa-live-widget/releases")))
        }
        button("Privacy policy", R.id.privacy_policy, help) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.geekprotocol.xyz/tools/kaspa-widget/privacy/")))
        }
        ready = true
        val selection = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { if (ready) display() }
        }
        for (choice in listOf(appearance, currency, interval, chartRange, previewChoice)) choice.onItemSelectedListener = selection
        chart.setOnCheckedChangeListener { _, _ -> if (ready) display() }
        paintScreen()
        display()
    }

    override fun onSaveInstanceState(state: Bundle) {
        val draft = selected()
        state.putString("draft_currency", draft.currency); state.putLong("draft_interval", draft.intervalMinutes)
        state.putString("draft_theme", draft.theme); state.putBoolean("draft_chart", draft.showChart)
        state.putInt("draft_background", draftColors.background); state.putInt("draft_text", draftColors.text); state.putInt("draft_accent", draftColors.accent)
        for ((key, color) in listOf("clock" to draftColors.clock, "price" to draftColors.price,
            "chart_color" to draftColors.chart, "network" to draftColors.network)) if (color != null) state.putInt("draft_$key", color)
        state.putInt("draft_intensity", intensity.progress)
        state.putString("draft_chart_range", draft.chartRange)
        state.putInt("preview_size", previewChoice.selectedItemPosition)
        state.putBoolean("help_open", findViewById<View>(R.id.source_help).visibility == View.VISIBLE)
        super.onSaveInstanceState(state)
    }
    override fun onResume() { super.onResume(); handler.removeCallbacks(update); handler.post(update) }
    override fun onPause() { handler.removeCallbacks(update); super.onPause() }

    private fun draftColor(state: Bundle?, key: String, fallback: Int?): Int? =
        if (state != null) { if (state.containsKey("draft_$key")) state.getInt("draft_$key") else null } else fallback

    private fun detailColor(key: String) = when (key) {
        "clock" -> draftColors.clock ?: draftColors.accent
        "price" -> draftColors.price ?: draftColors.accent
        "chart" -> draftColors.chart ?: draftColors.accent
        else -> draftColors.network ?: draftColors.text
    }

    private fun savePreset() {
        val input = EditText(this).apply { id = R.id.preset_name; hint = "Look name"; setSingleLine(); filters = arrayOf(InputFilter.LengthFilter(32)) }
        val dialog = AlertDialog.Builder(this).setTitle("Save this look")
            .setMessage("Save up to 20 looks. Using an existing name replaces that look.")
            .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save look", null).create()
        dialog.show(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (WidgetPresets(this).save(input.text.toString(), selected())) {
                dialog.dismiss(); display(); Toast.makeText(this, "Look saved on this device", Toast.LENGTH_SHORT).show()
            } else input.error = "Enter a name; at 20 looks, replace an existing name or delete one."
        }
    }

    private fun choosePreset(delete: Boolean) {
        val presets = WidgetPresets(this).all()
        if (presets.isEmpty()) { Toast.makeText(this, "Save a look first", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle(if (delete) "Delete a saved look" else "Load a saved look")
            .setItems(presets.map { it.name }.toTypedArray()) { _, index ->
                val preset = presets[index]
                if (delete) AlertDialog.Builder(this).setTitle("Delete ${preset.name}?")
                    .setMessage("Your applied widget settings will stay as they are.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete look") { _, _ ->
                        WidgetPresets(this).delete(preset.name); display()
                    }.show()
                else {
                    val draft = preset.applyTo(selected())
                    draftColors = draft.customColors
                    appearance.setSelection(WidgetPreferences.THEMES.indexOf(draft.theme))
                    intensity.progress = draft.chartIntensity; display()
                    Toast.makeText(this, "Look loaded · Apply settings to use it", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun selected() = WidgetPreferences(WidgetPreferences.CURRENCIES[currency.selectedItemPosition],
        WidgetPreferences.INTERVALS[interval.selectedItemPosition], WidgetPreferences.THEMES[appearance.selectedItemPosition], chart.isChecked, draftColors, intensity.progress,
        ChartRange.values()[chartRange.selectedItemPosition].key)

    private fun save() {
        val selected = selected()
        if (selected != WidgetSettings(this).get()) {
            WidgetSettings(this).save(selected)
            WidgetRenderer.updateAll(this)
            if (WorkScheduler.hasWidgets(this)) WorkScheduler.scheduleWidgetUpdates(this, true)
            else WorkScheduler.refreshNow(this, true)
        }
        display()
        Toast.makeText(this, "Settings saved for all widgets", Toast.LENGTH_SHORT).show()
    }

    private fun pin(large: Boolean) {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            val component = ComponentName(this, if (large) KaspaWidgetLargeProvider::class.java else KaspaWidgetProvider::class.java)
            if (!manager.requestPinAppWidget(component, null, null)) Toast.makeText(this, "Use your home screen’s Widgets menu", Toast.LENGTH_LONG).show()
        } else Toast.makeText(this, "Long-press your home screen → Widgets → Kaspa Live Widget", Toast.LENGTH_LONG).show()
    }

    private fun display() {
        if (!ready) return
        val saved = WidgetSettings(this).get(); val draft = selected()
        val update = if (AppUpdates.enabled(this)) UpdateStore(this).available() else null
        findViewById<Button>(R.id.app_updates).text = update?.let { "Update available · ${it.versionName}" } ?: "App updates"
        if (activeTheme != draft.theme) { activeTheme = draft.theme; paintScreen() }
        colorControls.visibility = if (draft.theme == "custom") View.VISIBLE else View.GONE
        backgroundColorButton.text = "Background · ${ColorEditor.hex(draftColors.background)}"
        textColorButton.text = "Text · ${ColorEditor.hex(draftColors.text)}"
        accentColorButton.text = "Accent · ${ColorEditor.hex(draftColors.accent)}"
        for ((key, button) in detailColors) button.text = "${if (key == "network") "Network stats" else key.replaceFirstChar { it.uppercase() }} · ${ColorEditor.hex(detailColor(key))}"
        intensityLabel.text = "Chart intensity · ${intensity.progress}% · ${if (intensity.progress == 0) "hidden" else if (intensity.progress <= 50) "subtle" else "bright"}"
        intensity.contentDescription = "Chart intensity ${intensity.progress} percent"
        chartRange.isEnabled = chart.isChecked; chartRange.alpha = if (chart.isChecked) 1f else .45f
        intensity.isEnabled = chart.isChecked; intensity.alpha = if (chart.isChecked) 1f else .45f
        presetNote.text = "${WidgetPresets(this).all().size}/20 saved looks · Saving a look does not apply widget settings."
        colorNote.text = if (ColorUtils.calculateContrast(draftColors.text, draftColors.background) < 4.5 ||
            listOf(draftColors.accent, detailColor("clock"), detailColor("price"), detailColor("network"))
                .any { ColorUtils.calculateContrast(it, draftColors.background) < 4.5 })
            "Low contrast in this palette. Choose lighter or darker text/accent colors to make readings easier to see."
            else "Preview your colors above. Apply settings to save them for all widgets."
        val status = RefreshStatusCache(this).get()
        val savedMarket = MarketCache(this).get(saved.currency)
        val savedNetwork = DataCache(this).getCachedData() ?: KaspaNetworkData()
        val feedback = RefreshFeedback.caption(status, savedMarket, savedNetwork, ConnectionState.offline(this))
        reading.text = "${saved.currency} · $feedback\n${MarketDisplay.price(savedMarket)} · ${MarketDisplay.change(savedMarket)}\n${WidgetFreshness.label(savedMarket.timestamp)}\n${savedMarket.source.ifBlank { "No price feed saved yet" }}\n${RefreshFeedback.ages(savedMarket, savedNetwork)}\nTap Refresh to retry; an outage keeps the last saved readings."
        val dirty = draft != saved
        draftFeedback.text = if (dirty) "Unsaved changes · Apply settings to update all widgets." else "Your choices are saved for all widgets."
        applyButton.isEnabled = dirty; applyButton.alpha = if (dirty) 1f else .55f
        refreshButton.isEnabled = !status.inProgress(); refreshButton.alpha = if (status.inProgress()) .55f else 1f
        refreshButton.text = if (status.inProgress()) status.label(saved.currency) else "Refresh saved settings"
        val range = ChartRange.fromKey(draft.chartRange)
        val market = MarketCache(this).get(draft.currency, range)
        val data = DataCache(this).getCachedData() ?: KaspaNetworkData()
        previewNote.text = when {
            market.timestamp <= 0 -> "No saved ${draft.currency} quote yet. Apply settings, then refresh."
            !draft.showChart || draft.chartIntensity == 0 -> "Preview uses saved ${draft.currency} readings. Chart is off."
            else -> "Saved ${draft.currency} readings · ${MarketDisplay.historyLabel(market, range = range)}"
        }
        val large = previewChoice.selectedItemPosition == 1
        val key = listOf(draft, large, preview.width, market, data, status.label(draft.currency), status.inProgress(), ConnectionState.offline(this),
            WidgetFreshness.label(data.copy(price = market.price, priceTimestamp = market.timestamp)),
            WidgetFreshness.hashrateLabel(data), MarketDisplay.change(market), MarketDisplay.chartCaption(market, range = range))
        if (key != previewState) {
            previewState = key
            val height = if (large) 300 else 180; val width = if (large) 250 else 140
            val widget = WidgetRenderer.views(this, large, height, width, draft).apply(this, preview)
            // Keep the actual launcher layout proportions, but fit its thumbnail
            // inside the measured card rather than cropping a fixed-width preview.
            val scale = if (preview.width > 0) (preview.width.toFloat() / dp(width)).coerceAtMost(1f) else 1f
            widget.pivotX = 0f; widget.pivotY = 0f
            widget.scaleX = scale; widget.scaleY = scale
            fun readOnly(view: View) {
                view.isClickable = false; view.isFocusable = false
                if (view is ViewGroup) for (index in 0 until view.childCount) readOnly(view.getChildAt(index))
            }
            readOnly(widget)
            preview.removeAllViews()
            preview.addView(widget, FrameLayout.LayoutParams(dp(width), dp(height), Gravity.TOP or Gravity.LEFT).apply {
                leftMargin = ((preview.width - dp(width) * scale) / 2).toInt().coerceAtLeast(0)
                topMargin = dp(8)
            })
            preview.layoutParams.height = (dp(height) * scale).toInt() + dp(16)
            preview.requestLayout()
        }
    }

    private fun text(value: String, parent: LinearLayout, role: String = "text") = TextView(this).apply {
        text = value; tag = role; textSize = if (role == "secondary") 13f else 15f
        typeface = Typeface.MONOSPACE; setPadding(0, dp(6), 0, dp(6)); parent.addView(this)
    }
    private fun section(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; tag = "card"; setPadding(dp(12), dp(12), dp(12), dp(12))
        body.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        text(title, this, "heading").textSize = 13f
    }
    private fun select(title: String, entries: List<String>, id: Int, parent: LinearLayout): Spinner {
        text(title, parent, "secondary").labelFor = id
        return Spinner(this).apply {
            this.id = id; tag = "control"; setPadding(dp(8), 0, dp(8), 0)
            adapter = object : ArrayAdapter<String>(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, entries) {
                private fun style(view: View) = (view as TextView).apply {
                    setTextColor(ink); textSize = 15f; typeface = Typeface.MONOSPACE
                    setBackgroundColor(if (xray) Color.rgb(24,24,24) else if (light) Color.rgb(226, 245, 243) else Color.rgb(18, 40, 53))
                }
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    style(super.getView(position, convertView, parent)).apply { text = "${getItem(position)}  ▾" }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = style(super.getDropDownView(position, convertView, parent))
            }
            parent.addView(this, LinearLayout.LayoutParams(-1, dp(48)))
        }
    }
    private fun button(value: String, id: Int, parent: LinearLayout, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; this.id = id; tag = if (primary) "primary" else "button"; isAllCaps = false
        typeface = Typeface.MONOSPACE; textSize = 13f; minHeight = dp(48)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        setOnClickListener { action() }
    }
    private fun paintScreen() {
        scroll.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            if (xray) intArrayOf(Color.BLACK, 0xFF181818.toInt()) else if (light) intArrayOf(0xFFF1FBFA.toInt(), 0xFFC5DEDF.toInt()) else intArrayOf(0xFF050C18.toInt(), 0xFF122835.toInt()))
        window.statusBarColor = if (xray) Color.BLACK else if (light) 0xFFF1FBFA.toInt() else 0xFF050C18.toInt()
        findViewById<ImageView>(R.id.setup_logo).apply {
            if (xray) setImageBitmap(WidgetPalette.from(WidgetPreferences(theme = "xray")).logo(this@SettingsActivity))
            else setImageResource(R.drawable.kaspa_mark)
        }
        window.navigationBarColor = window.statusBarColor
        WindowCompat.getInsetsController(window, scroll).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        chart.thumbTintList = ColorStateList.valueOf(accent)
        chart.trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accent, muted))
        fun paint(view: View) {
            if (view.id == R.id.widget_preview) return
            if (view is TextView) view.setTextColor(when (view.tag) {
                "heading", "title", "primary" -> accent
                "secondary" -> muted
                else -> ink
            })
            if (view.tag in listOf("card", "control", "button", "primary")) {
                val shape = GradientDrawable().apply {
                    setColor(if (xray) 0xD0181818.toInt() else if (light) 0xA8FFFFFF.toInt() else 0xB0182D42.toInt()); cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), if (xray) 0x80FFFFFF.toInt() else if (light) 0x80609597.toInt() else 0x6080E6DE)
                }
                view.background = if (view is Button) RippleDrawable(ColorStateList.valueOf(if (xray) 0x30FFFFFF else 0x3080E6DE), shape, null) else shape
            }
            if (view is Spinner) (view.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
            if (view is ViewGroup) for (index in 0 until view.childCount) paint(view.getChildAt(index))
        }
        paint(body)
    }
}
