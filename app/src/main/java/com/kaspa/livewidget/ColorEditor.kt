package com.kaspa.livewidget

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.core.graphics.ColorUtils
import java.util.Locale

/** Native RGB controls plus exact hex entry; cancel never changes the draft. */
internal object ColorEditor {
    fun hex(color: Int) = String.format(Locale.US, "#%06X", color and 0xFFFFFF)
    fun parse(value: String): Int? = value.trim().takeIf { it.matches(Regex("#?[0-9a-fA-F]{6}")) }
        ?.let { Color.parseColor("#" + it.removePrefix("#")) }

    fun show(activity: Activity, title: String, initial: Int, chosen: (Int) -> Unit) {
        fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(12)) }
        val swatch = TextView(activity).apply { gravity = Gravity.CENTER; textSize = 18f }
        body.addView(swatch, LinearLayout.LayoutParams(-1, dp(56)))
        body.addView(TextView(activity).apply { text = "Hex color · #RRGGBB"; labelFor = R.id.color_hex; setPadding(0, dp(12), 0, 0) })
        val input = EditText(activity).apply {
            id = R.id.color_hex; setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(7)); setText(hex(initial))
        }
        body.addView(input)
        var current = initial
        val names = listOf("Red", "Green", "Blue")
        val ids = listOf(R.id.color_red, R.id.color_green, R.id.color_blue)
        val labels = names.map { name -> TextView(activity).apply { text = name; body.addView(this) } }
        val sliders = names.mapIndexed { index, name -> SeekBar(activity).apply {
            id = ids[index]; max = 255; contentDescription = name
            body.addView(this, body.indexOfChild(labels[index]) + 1, LinearLayout.LayoutParams(-1, dp(48)))
        } }
        fun render() {
            val rgb = listOf(Color.red(current), Color.green(current), Color.blue(current))
            for (i in 0..2) { sliders[i].progress = rgb[i]; labels[i].text = "${names[i]}: ${rgb[i]}" }
            swatch.setBackgroundColor(current); swatch.text = hex(current)
            swatch.setTextColor(if (ColorUtils.calculateLuminance(current) > .45) Color.BLACK else Color.WHITE)
        }
        for (slider in sliders) slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(view: SeekBar?) = Unit
            override fun onStopTrackingTouch(view: SeekBar?) = Unit
            override fun onProgressChanged(view: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                current = Color.rgb(sliders[0].progress, sliders[1].progress, sliders[2].progress)
                input.setText(hex(current)); input.setSelection(input.length()); render()
            }
        })
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(value: Editable?) { parse(value.toString())?.let { current = it; input.error = null; render() } }
        })
        render()
        val dialog = AlertDialog.Builder(activity).setTitle(title)
            .setView(ScrollView(activity).apply { addView(body) })
            .setNegativeButton("Cancel", null).setPositiveButton("Use color", null).create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val color = parse(input.text.toString())
            if (color == null) input.error = "Enter six hex digits, such as #FFFFFF"
            else { chosen(color); dialog.dismiss() }
        }
    }
}
