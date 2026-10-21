package com.kaspa.livewidget.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.kaspa.livewidget.utils.WorkScheduler
import com.kaspa.livewidget.widget.WidgetRenderer
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

internal object UpdateScheduler {
    const val CHECK = "kaspa_app_update_check"
    const val DOWNLOAD = "kaspa_app_update_download"
    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        if (!AppUpdates.enabled(context) || !UpdateStore(context).automatic) {
            manager.cancelUniqueWork(CHECK); return
        }
        manager.enqueueUniquePeriodicWork(CHECK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build())
    }
    fun download(context: Context, release: AppRelease) {
        require(AppUpdates.enabled(context) && release.newerThan(com.kaspa.livewidget.BuildConfig.VERSION_NAME))
        WorkManager.getInstance(context).enqueueUniqueWork(DOWNLOAD, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(workDataOf("release" to release.json()))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build())
    }
}

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (AppUpdates.enabled(applicationContext) && UpdateStore(applicationContext).automatic) {
            AppUpdates.check(applicationContext)
            WidgetRenderer.updateAll(applicationContext)
        }
        return Result.success()
    }
}

class UpdateDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AppUpdates.enabled(applicationContext)) return Result.failure()
        val release = AppRelease.cached(inputData.getString("release"))
            ?.takeIf { it.newerThan(com.kaspa.livewidget.BuildConfig.VERSION_NAME) } ?: return Result.failure()
        val store = UpdateStore(applicationContext)
        val file = AppUpdates.apk(applicationContext)
        store.ready(null)
        return try {
            setProgress(workDataOf("percent" to 0))
            UpdateSource().download(release, file) { percent -> setProgress(workDataOf("percent" to percent)) }
            ApkUpdateVerifier.verify(applicationContext, file, release)
            store.ready(release)
            Result.success()
        } catch (e: CancellationException) {
            file.delete(); throw e
        } catch (_: Exception) {
            file.delete()
            Result.failure(workDataOf("error" to "The update could not be downloaded and verified. Check your connection and try again."))
        }
    }
}

class AppUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED && WorkScheduler.hasWidgets(context)) {
            WorkScheduler.scheduleWidgetUpdates(context, true)
        }
    }
}
