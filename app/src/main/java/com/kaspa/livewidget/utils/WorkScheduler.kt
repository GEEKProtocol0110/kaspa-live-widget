package com.kaspa.livewidget.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kaspa.livewidget.worker.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling widget updates using WorkManager
 */
object WorkScheduler {

    private const val WORK_NAME = "kaspa_widget_update"
    // Update interval: 15 minutes balances freshness with battery usage
    // Users concerned about battery can adjust via Android's battery optimization settings
    private const val UPDATE_INTERVAL_MINUTES = 15L

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
    }

    /**
     * Cancel scheduled widget updates
     */
    fun cancelWidgetUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
