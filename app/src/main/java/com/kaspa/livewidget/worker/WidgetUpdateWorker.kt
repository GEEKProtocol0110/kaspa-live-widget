package com.kaspa.livewidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kaspa.livewidget.api.KaspaApiService
import com.kaspa.livewidget.utils.DataCache
import com.kaspa.livewidget.widget.KaspaWidgetProvider
import com.kaspa.livewidget.widget.KaspaWidgetLargeProvider

/**
 * Worker for updating widget data in the background
 * Scheduled by WorkManager to run every 15-30 minutes
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val apiService = KaspaApiService()
    private val cache = DataCache(context)

    override suspend fun doWork(): Result {
        return try {
            // Fetch latest data from API
            val data = apiService.fetchAllData(cache.getCachedData())
            
            if (data != null) cache.saveData(data)
            
            // Update all widget instances
            updateWidgets()
            
            if (data == null) Result.retry() else Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        
        // Update 2x2 widgets
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, KaspaWidgetProvider::class.java)
        )
        KaspaWidgetProvider.updateAllWidgets(applicationContext, appWidgetManager, widgetIds)
        
        // Update 4x2 widgets
        val largeWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(applicationContext, KaspaWidgetLargeProvider::class.java)
        )
        KaspaWidgetLargeProvider.updateAllWidgets(applicationContext, appWidgetManager, largeWidgetIds)
    }
}
