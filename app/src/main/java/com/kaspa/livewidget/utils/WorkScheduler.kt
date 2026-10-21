package com.kaspa.livewidget.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.*
import com.kaspa.livewidget.widget.*
import com.kaspa.livewidget.worker.WidgetUpdateWorker
import com.kaspa.livewidget.update.UpdateScheduler
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_NAME = "kaspa_widget_update"
    private const val INITIAL_WORK_NAME = "kaspa_widget_initial_update"

    fun refreshNow(context: Context, settingsChanged: Boolean = false) {
        val cache = RefreshStatusCache(context)
        if (!cache.get().inProgress() || settingsChanged) cache.set("queued", WidgetSettings(context).get().currency)
        WidgetRenderer.updateAll(context)
        WorkManager.getInstance(context).enqueueUniqueWork(INITIAL_WORK_NAME,
            if (settingsChanged) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build())
    }

    fun scheduleWidgetUpdates(context: Context, settingsChanged: Boolean = false) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(WidgetSettings(context).get().intervalMinutes.toLong(), TimeUnit.MINUTES).build())
        UpdateScheduler.schedule(context)
        refreshNow(context, settingsChanged)
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, KaspaWidgetProvider::class.java)).isNotEmpty() ||
            manager.getAppWidgetIds(ComponentName(context, KaspaWidgetLargeProvider::class.java)).isNotEmpty()
    }

    fun cancelWidgetUpdatesIfUnused(context: Context) {
        if (!hasWidgets(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(INITIAL_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(UpdateScheduler.CHECK)
        }
    }
}
