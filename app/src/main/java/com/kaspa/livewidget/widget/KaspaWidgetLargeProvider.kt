package com.kaspa.livewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kaspa.livewidget.R
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.utils.DataCache
import com.kaspa.livewidget.utils.WorkScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widget provider for 4x4 Kaspa widget
 * Displays time, last updated, price, DAA score, observed BPS, and hashrate
 */
class KaspaWidgetLargeProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) WorkScheduler.refreshNow(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Schedule periodic updates with WorkManager
        WorkScheduler.scheduleWidgetUpdates(context)
        
        // Update all widget instances
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // First widget added, schedule updates
        WorkScheduler.scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed, cancel updates
        WorkScheduler.cancelWidgetUpdatesIfUnused(context)
    }

    companion object {
        private const val ACTION_REFRESH = "com.kaspa.livewidget.REFRESH_LARGE"
        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val cache = DataCache(context)
            val data = cache.getCachedData() ?: KaspaNetworkData()

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, data)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            data: KaspaNetworkData
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout_large)
            val refresh = Intent(context, KaspaWidgetLargeProvider::class.java).setAction(ACTION_REFRESH)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getBroadcast(context, 0, refresh, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Update time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            views.setTextViewText(R.id.widget_time, timeFormat.format(Date()))

            // Update "last updated" indicator
            views.setTextViewText(R.id.widget_updated, WidgetFreshness.label(data))

            // Update price
            val priceText = if (data.price > 0) {
                String.format("$%.4f", data.price)
            } else {
                "$--"
            }
            views.setTextViewText(R.id.widget_price, priceText)

            // Update block height
            val blockHeightText = if (data.blockHeight > 0) {
                String.format("%,d", data.blockHeight)
            } else {
                "--"
            }
            views.setTextViewText(R.id.widget_block_height, blockHeightText)

            // Update BPS
            val bpsText = if (data.bps > 0) {
                String.format("%.1f", data.bps)
            } else {
                "--"
            }
            views.setTextViewText(R.id.widget_bps, bpsText)

            // Update hashrate
            views.setTextViewText(R.id.widget_hashrate, data.hashrate)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
