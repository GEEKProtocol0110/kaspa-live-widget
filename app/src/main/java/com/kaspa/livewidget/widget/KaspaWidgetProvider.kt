package com.kaspa.livewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kaspa.livewidget.utils.WorkScheduler

class KaspaWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) WorkScheduler.refreshNow(context)
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WorkScheduler.scheduleWidgetUpdates(context)
        updateAllWidgets(context, manager, ids)
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateAllWidgets(context, manager, intArrayOf(id))
    }
    override fun onEnabled(context: Context) = WorkScheduler.scheduleWidgetUpdates(context)
    override fun onDisabled(context: Context) = WorkScheduler.cancelWidgetUpdatesIfUnused(context)
    companion object {
        const val ACTION_REFRESH = "com.kaspa.livewidget.REFRESH_SMALL"
        fun updateAllWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            for (id in ids) manager.updateAppWidget(id, WidgetRenderer.views(context, false,
                manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)))
        }
    }
}
