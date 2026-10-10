package com.kaspa.livewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.kaspa.livewidget.R
import com.kaspa.livewidget.SettingsActivity
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.utils.*
import java.util.Locale

object WidgetRenderer {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        KaspaWidgetProvider.updateAllWidgets(context, manager, manager.getAppWidgetIds(ComponentName(context, KaspaWidgetProvider::class.java)))
        KaspaWidgetLargeProvider.updateAllWidgets(context, manager, manager.getAppWidgetIds(ComponentName(context, KaspaWidgetLargeProvider::class.java)))
    }

    fun views(context: Context, large: Boolean, minHeight: Int = 0, minWidth: Int = 0,
              previewPreferences: WidgetPreferences? = null): RemoteViews {
        val prefs = previewPreferences?.normalized() ?: WidgetSettings(context).get()
        val data = DataCache(context).getCachedData() ?: KaspaNetworkData()
        val market = MarketCache(context).get(prefs.currency)
        val status = RefreshStatusCache(context).get()
        val views = RemoteViews(context.packageName, if (large) R.layout.widget_layout_large else R.layout.widget_layout)
        val refresh = if (large) Intent(context, KaspaWidgetLargeProvider::class.java).setAction(KaspaWidgetLargeProvider.ACTION_REFRESH)
            else Intent(context, KaspaWidgetProvider::class.java).setAction(KaspaWidgetProvider.ACTION_REFRESH)
        views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getBroadcast(context, 0, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(context, 0,
            Intent(context, SettingsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val fullStatus = status.label(prefs.currency)
        val statusCaption = when (fullStatus) {
            "Some feeds delayed · cached readings kept" -> "Some feeds delayed"
            "Update unavailable · cached readings kept" -> "Update unavailable"
            "Update interrupted · tap to retry" -> "Tap to retry"
            else -> fullStatus
        }
        views.setTextViewText(R.id.widget_status, statusCaption)
        views.setContentDescription(R.id.widget_status, fullStatus)
        views.setViewVisibility(R.id.widget_progress, if (status.inProgress()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_price, MarketDisplay.price(market))
        views.setTextViewText(R.id.widget_change, MarketDisplay.change(market))
        val snapshot = data.copy(price = market.price, priceTimestamp = market.timestamp)
        views.setTextViewText(R.id.widget_updated, WidgetFreshness.label(snapshot))
        views.setTextViewText(R.id.widget_block_height, if (data.blockHeight > 0) String.format(Locale.getDefault(), "%,d", data.blockHeight) else "--")
        val light = prefs.theme == "light"
        val textColor = if (light) Color.rgb(12, 43, 55) else Color.rgb(222, 255, 249)
        val accent = if (light) Color.rgb(9, 86, 91) else Color.rgb(120, 255, 224)
        val secondary = if (light) Color.rgb(47, 77, 85) else Color.rgb(181, 218, 219)
        val width = ((minWidth.takeIf { it > 0 } ?: if (large) 250 else 140) * 2).coerceIn(280,700)
        val height = ((minHeight.takeIf { it > 0 } ?: if (large) 300 else 180) * 2).coerceIn(360,900)
        views.setImageViewBitmap(R.id.widget_chart, PriceChart.glass(if (prefs.showChart) market.history else emptyList(),light,width,height))
        views.setViewVisibility(R.id.widget_chart_group, if (prefs.showChart) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_chart_label, MarketDisplay.chartCaption(market))
        views.setContentDescription(R.id.widget_chart_label, MarketDisplay.historyLabel(market))
        views.setInt(R.id.widget_root, "setBackgroundResource", if (light) R.drawable.widget_background_light else R.drawable.widget_background)
        for (id in listOf(R.id.widget_time, R.id.widget_price, R.id.widget_change, R.id.widget_block_height,
            R.id.widget_updated, R.id.widget_status, R.id.widget_settings, R.id.widget_brand, R.id.widget_chart_label)) views.setTextColor(id, textColor)
        views.setTextColor(R.id.widget_price, accent)
        views.setTextColor(R.id.widget_time, accent)
        views.setTextColor(R.id.widget_brand, accent)
        val change = market.change24h?.takeIf { it.isFinite() && it >= -100 && market.changeTimestamp > 0 }
        views.setTextColor(R.id.widget_change, when {
            change == null || change == 0.0 -> secondary
            change < 0 -> if (light) Color.rgb(151, 35, 52) else Color.rgb(255, 178, 178)
            else -> accent
        })
        for (id in listOf(R.id.widget_updated, R.id.widget_status, R.id.widget_chart_label))
            views.setTextColor(id, secondary)
        for (id in listOf(R.id.price_panel, R.id.network_panel, R.id.time_panel)) views.setInt(id, "setBackgroundResource",
            if (light) R.drawable.data_panel_background_light else R.drawable.data_panel_background)
        views.setTextColor(R.id.network_panel_label, secondary)
        if (large) {
            for (id in listOf(R.id.bps_panel, R.id.hash_panel)) views.setInt(id, "setBackgroundResource",
                if (light) R.drawable.data_panel_background_light else R.drawable.data_panel_background)
            for (id in listOf(R.id.bps_panel_label, R.id.hash_label)) views.setTextColor(id, secondary)
            views.setTextViewText(R.id.widget_bps, if (data.bps > 0) String.format(Locale.getDefault(), "%.1f", data.bps) else "--")
            views.setTextViewText(R.id.widget_hashrate, data.hashrate)
            views.setTextViewText(R.id.widget_hashrate_updated, WidgetFreshness.hashrateLabel(data))
            for (id in listOf(R.id.widget_bps, R.id.widget_hashrate)) views.setTextColor(id, textColor)
            views.setTextColor(R.id.widget_hashrate_updated, secondary)

        }
        return views
    }
}
