package com.kaspa.livewidget.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kaspa.livewidget.widget.KaspaWidgetLargeProvider
import com.kaspa.livewidget.widget.KaspaWidgetProvider
import com.kaspa.livewidget.worker.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling widget updates using WorkManager
 */
object WorkScheduler {

    private const val WORK_NAME = "kaspa_widget_update"
    private const val INITIAL_WORK_NAME = "kaspa_widget_initial_update"
    // Update interval: 15 minutes balances freshness with battery usage
    // Users concerned about battery can adjust via Android's battery optimization settings
    private const val UPDATE_INTERVAL_MINUTES = 15L

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            INITIAL_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        )
    }

    /**
     * Schedule periodic widget updates
     */
    fun scheduleWidgetUpdates(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        // A newly added widget should fetch immediately, rather than wait for
        // the first periodic run while it shows empty placeholders.
        WorkManager.getInstance(context).enqueueUniqueWork(
            INITIAL_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        )
    }

    /**
     * Cancel scheduled widget updates
     */
    fun cancelWidgetUpdatesIfUnused(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val small = manager.getAppWidgetIds(ComponentName(context, KaspaWidgetProvider::class.java))
        val large = manager.getAppWidgetIds(ComponentName(context, KaspaWidgetLargeProvider::class.java))
        if (small.isEmpty() && large.isEmpty()) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(INITIAL_WORK_NAME)
        }
    }
}
