package com.kaspa.livewidget.update

import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.kaspa.livewidget.BuildConfig
import com.kaspa.livewidget.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

class OfflineUpdateActivity : AppUpdateActivity() {
    override fun automaticCheck() = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class AppUpdateUiTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val release get() = AppRelease("1.7.0", AppRelease.url("1.7.0"), "a".repeat(64), 100)
    @Before fun isolateWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setTaskExecutor(SynchronousExecutor()).setExecutor(Executor { }).build(),
            WorkManagerTestInitHelper.ExecutorsMode.PRESERVE_EXECUTORS)
    }
    @Test fun failedChecksPreserveLastGoodReleaseAndNeverAdvanceSuccessTime() {
        val store = UpdateStore(context)
        store.checked(release, 1000)
        store.checked(null, 2000)
        assertEquals(release, store.release)
        if (AppUpdates.enabled(context)) assertEquals(release, store.available()) else assertNull(store.available())
        assertEquals(1000L, store.lastSuccess)
        assertEquals(2000L, store.lastAttempt)
        store.automatic(false)
        UpdateScheduler.schedule(context)
        assertFalse(store.automatic)
        assertTrue(WorkManager.getInstance(context).getWorkInfosForUniqueWork(UpdateScheduler.CHECK).get().none { !it.state.isFinished })
    }
    @Test fun malformedCachedMetadataCannotBecomeAnInstallOffer() {
        context.getSharedPreferences("kaspa_app_updates", 0).edit().putString("release", "invalid").commit()
        assertNull(UpdateStore(context).available())
        assertNull(AppRelease.cached("""{"version":"1.7.0","url":"https://evil.example/update.apk","sha256":"${"a".repeat(64)}","size":100}"""))
    }
    @Test fun updateScreenShowsSavedOfferAndInstallationRequiresAnExplicitAction() {
        UpdateStore(context).checked(release, System.currentTimeMillis())
        Robolectric.buildActivity(OfflineUpdateActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val enabled = AppUpdates.enabled(activity)
            val status = activity.findViewById<TextView>(R.id.app_update_status).text.toString()
            assertTrue(status, status.contains(if (enabled) "1.7.0" else "debug"))
            assertEquals("Update now", activity.findViewById<Button>(R.id.app_update_action).text.toString())
            assertEquals(enabled, activity.findViewById<Switch>(R.id.app_update_automatic).isEnabled)
            assertTrue(WorkManager.getInstance(context).getWorkInfosForUniqueWork(UpdateScheduler.DOWNLOAD).get().isEmpty())
        }
    }
}
