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

    fun views(context: Context, large: Boolean, minHeight: Int = 0): RemoteViews {
        val prefs = WidgetSettings(context).get()
        val data = DataCache(context).getCachedData() ?: KaspaNetworkData()
        val market = MarketCache(context).get(prefs.currency)
        val status = RefreshStatusCache(context).get()
        val views = RemoteViews(context.packageName, if (large) R.layout.widget_layout_large else R.layout.widget_layout)
        val refresh = if (large) Intent(context, KaspaWidgetLargeProvider::class.java).setAction(KaspaWidgetLargeProvider.ACTION_REFRESH)
            else Intent(context, KaspaWidgetProvider::class.java).setAction(KaspaWidgetProvider.ACTION_REFRESH)
        views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getBroadcast(context, 0, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(context, 0,
            Intent(context, SettingsActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setTextViewText(R.id.widget_status, status.label(prefs.currency).replace("Some feeds delayed · cached readings kept", "Partial update · cache kept").replace("Update unavailable · cached readings kept", "Update unavailable · cache kept").replace("Update interrupted · tap to retry", "Interrupted · tap to retry"))
        views.setViewVisibility(R.id.widget_progress, if (status.inProgress()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_price, MarketDisplay.price(market))
        views.setTextViewText(R.id.widget_change, MarketDisplay.change(market))
        val snapshot = data.copy(price = market.price, priceTimestamp = market.timestamp)
        views.setTextViewText(R.id.widget_updated, WidgetFreshness.label(snapshot))
        views.setTextViewText(R.id.widget_block_height, if (data.blockHeight > 0) String.format(Locale.getDefault(), "%,d", data.blockHeight) else "--")
        val light = prefs.theme == "light"
        val textColor = if (light) Color.rgb(16, 41, 45) else Color.WHITE
        views.setInt(R.id.widget_root, "setBackgroundResource", if (light) R.drawable.widget_background_light else R.drawable.widget_background)
        for (id in listOf(R.id.widget_time, R.id.widget_price, R.id.widget_change, R.id.widget_block_height,
            R.id.widget_updated, R.id.widget_status, R.id.widget_settings)) views.setTextColor(id, textColor)
        for (id in listOf(R.id.price_panel, R.id.network_panel)) views.setInt(id, "setBackgroundResource",
            if (light) R.drawable.data_panel_background_light else R.drawable.data_panel_background)
        views.setTextColor(R.id.network_panel_label, textColor)
        if (large) {
            for (id in listOf(R.id.bps_panel, R.id.hash_panel)) views.setInt(id, "setBackgroundResource",
                if (light) R.drawable.data_panel_background_light else R.drawable.data_panel_background)
            for (id in listOf(R.id.bps_panel_label, R.id.hash_label)) views.setTextColor(id, textColor)
            views.setTextViewText(R.id.widget_bps, if (data.bps > 0) String.format(Locale.getDefault(), "%.1f", data.bps) else "--")
            views.setTextViewText(R.id.widget_hashrate, data.hashrate)
            views.setTextViewText(R.id.widget_hashrate_updated, WidgetFreshness.hashrateLabel(data))
            for (id in listOf(R.id.widget_bps, R.id.widget_hashrate, R.id.widget_hashrate_updated, R.id.widget_chart_label)) views.setTextColor(id, textColor)
            val show = prefs.showChart && minHeight >= 380
            views.setViewVisibility(R.id.widget_chart_group, if (show) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_chart_label, MarketDisplay.historyLabel(market))
            views.setViewVisibility(R.id.widget_chart, if (market.history.size >= 2) View.VISIBLE else View.GONE)
            if (show && market.history.size >= 2) views.setImageViewBitmap(R.id.widget_chart, PriceChart.bitmap(market.history, textColor))
            views.setContentDescription(R.id.widget_chart, MarketDisplay.historyLabel(market))
        }
        return views
    }
}
