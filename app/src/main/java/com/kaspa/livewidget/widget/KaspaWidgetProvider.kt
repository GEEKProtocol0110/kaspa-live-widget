package com.kaspa.livewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.kaspa.livewidget.R
import com.kaspa.livewidget.data.KaspaNetworkData
import com.kaspa.livewidget.utils.DataCache
import com.kaspa.livewidget.utils.WorkScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widget provider for 2x2 Kaspa widget
 * Displays time, price, and block height
 */
class KaspaWidgetProvider : AppWidgetProvider() {

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
        WorkScheduler.cancelWidgetUpdates(context)
    }

    companion object {
        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val cache = DataCache(context)
            val data = cache.getValidCachedData() ?: KaspaNetworkData()

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
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Update time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            views.setTextViewText(R.id.widget_time, timeFormat.format(Date()))

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

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
