package com.kaspa.livewidget.update

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kaspa.livewidget.BuildConfig
import com.kaspa.livewidget.R
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

/** Downloads require a tap. Android owns permission and installation approval. */
open class AppUpdateActivity : Activity() {
    private val scope = MainScope()
    private lateinit var status: TextView
    private lateinit var check: Button
    private lateinit var update: Button
    private lateinit var progress: ProgressBar
    private var checking = false
    private var latestWork: WorkInfo? = null
    private var awaitingPermission = false
    private val observer = Observer<List<WorkInfo>> { work ->
        latestWork = work.firstOrNull { !it.state.isFinished } ?: work.firstOrNull()
        render()
    }
    private val feed by lazy { WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(UpdateScheduler.DOWNLOAD) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        awaitingPermission = state?.getBoolean("awaiting_permission") ?: false
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(5, 12, 24)); addView(body) }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom); insets
        }
        ViewCompat.requestApplyInsets(scroll)
        window.statusBarColor = Color.rgb(5, 12, 24); window.navigationBarColor = window.statusBarColor
        WindowCompat.getInsetsController(window, scroll).apply {
            isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false
        }
        label(body, "APP UPDATES", 22f)
        label(body, "Installed version ${BuildConfig.VERSION_NAME}")
        status = label(body, "", 16f).apply { id = R.id.app_update_status; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = R.id.app_update_progress; max = 100; progressTintList = ColorStateList.valueOf(Color.rgb(120, 255, 224))
            body.addView(this, LinearLayout.LayoutParams(-1, dp(16)))
        }
        check = button(body, "Check for updates", R.id.app_update_check) { checkForUpdates(true) }
        update = button(body, "Update now", R.id.app_update_action) { act() }
        val store = UpdateStore(this)
        val automatic = Switch(this).apply {
            text = "Check daily for new versions"; setTextColor(Color.rgb(222, 255, 249)); minHeight = dp(48)
            isChecked = AppUpdates.enabled(this@AppUpdateActivity) && store.automatic; isEnabled = AppUpdates.enabled(this@AppUpdateActivity)
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
            setOnCheckedChangeListener { _, value ->
                store.automatic(value); UpdateScheduler.schedule(this@AppUpdateActivity)
            }
        }
        automatic.id = R.id.app_update_automatic
        label(body, if (AppUpdates.enabled(this))
            "Daily checks contact GitHub’s public release service. Updates download only when you tap Update now. Your widget placement and saved settings stay in place. Android asks you to approve installation."
            else if (!BuildConfig.DIRECT_UPDATES) "Manage automatic updates in Google Play. Your widget settings stay in place."
            else "The signed public app provides built-in updates. The debug app installs separately.")
        button(body, "Release notes", R.id.app_update_notes) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppRelease.RELEASES))) }
            catch (_: ActivityNotFoundException) { Toast.makeText(this, "No browser is available", Toast.LENGTH_SHORT).show() }
        }
        button(body, "Back to widget setup", R.id.app_update_back) { finish() }
        if (AppUpdates.enabled(this)) {
            feed.observeForever(observer)
            UpdateScheduler.schedule(this)
        }
        render()
        automaticCheck()
    }

    protected open fun automaticCheck() = checkForUpdates(false)

    private fun checkForUpdates(force: Boolean) {
        if (!AppUpdates.enabled(this) || checking) return
        checking = true; render()
        scope.launch {
            try { AppUpdates.check(this@AppUpdateActivity, force) }
            finally { checking = false; if (!isDestroyed) render() }
        }
    }

    private fun activeDownload() = latestWork?.state?.isFinished == false
    private fun ready() = UpdateStore(this).ready?.takeIf {
        it.newerThan(BuildConfig.VERSION_NAME) && AppUpdates.apk(this).isFile
    }

    private fun render() {
        if (!::status.isInitialized) return
        val store = UpdateStore(this)
        val available = store.available()
        val ready = ready()
        val active = activeDownload()
        check.isEnabled = AppUpdates.enabled(this) && !checking && !active
        progress.visibility = if (active) View.VISIBLE else View.GONE
        progress.progress = latestWork?.progress?.getInt("percent", 0) ?: 0
        update.visibility = if (AppUpdates.enabled(this) && (available != null || ready != null || active)) View.VISIBLE else View.GONE
        update.isEnabled = AppUpdates.enabled(this) && !checking && !active
        update.text = if (ready != null) "Install update" else "Update now"
        status.text = when {
            !BuildConfig.DIRECT_UPDATES -> "This build receives updates through Google Play. Enable auto-update in its Play Store settings."
            !AppUpdates.enabled(this) -> "This is a separate debug app. Install the signed public app for built-in updates."
            active -> if (latestWork?.state == WorkInfo.State.RUNNING) "Downloading update · ${progress.progress}%" else "Update queued · waiting for a connection"
            checking -> "Checking for a new version…"
            ready != null -> "Version ${ready.versionName} is verified and ready. Tap Install update to continue."
            latestWork?.state == WorkInfo.State.FAILED && available != null -> latestWork?.outputData?.getString("error") ?: "The update was interrupted. Tap Update now to retry."
            available != null -> "Version ${available.versionName} is available." + if (store.lastAttempt > store.lastSuccess) " Latest check failed; this is the last saved release." else ""
            store.lastAttempt > store.lastSuccess -> "Could not check for updates. Your installed widget keeps working. Try again when connected."
            store.lastSuccess > 0 -> "You’re up to date. Last checked ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(store.lastSuccess))}."
            else -> "Check for a new version when you’re connected."
        }
        update.alpha = if (update.isEnabled) 1f else .55f
    }

    private fun act() {
        if (!AppUpdates.enabled(this)) return
        if (ready() != null) install() else UpdateStore(this).available()?.let {
            UpdateScheduler.download(this, it)
        }
    }

    private fun install() {
        if (!AppUpdates.enabled(this)) return
        val release = ready() ?: return
        scope.launch {
            update.isEnabled = false
            try {
                withContext(Dispatchers.IO) { ApkUpdateVerifier.verify(this@AppUpdateActivity, AppUpdates.apk(this@AppUpdateActivity), release) }
                if (!packageManager.canRequestPackageInstalls()) {
                    AlertDialog.Builder(this@AppUpdateActivity).setTitle("Allow widget updates")
                        .setMessage("Android needs your permission to install updates from Kaspa Live Widget. Enable Allow from this source, then return here. You’ll still approve each installation.")
                        .setNegativeButton("Not now", null).setPositiveButton("Open Android settings") { _, _ ->
                            try {
                                awaitingPermission = true
                                startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:$packageName")), 71)
                            } catch (_: ActivityNotFoundException) {
                                awaitingPermission = false
                                status.text = "Open Android Settings → Apps → Special app access → Install unknown apps → Kaspa Live Widget."
                            }
                        }.show()
                } else {
                    val uri = FileProvider.getUriForFile(this@AppUpdateActivity, "$packageName.updates", AppUpdates.apk(this@AppUpdateActivity))
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            } catch (e: CancellationException) { throw e
            } catch (_: ActivityNotFoundException) {
                status.text = "Android’s installer is unavailable. The verified update is saved; try again later."
            } catch (_: Exception) {
                UpdateStore(this@AppUpdateActivity).ready(null)
                AppUpdates.apk(this@AppUpdateActivity).delete()
                status.text = "The update could not be installed. Check your Android settings, then download it again."
            } finally { update.isEnabled = true }
        }
    }

    @Deprecated("Android installer approval remains user-driven")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 71 && awaitingPermission) {
            awaitingPermission = false
            if (packageManager.canRequestPackageInstalls()) install() else render()
        }
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putBoolean("awaiting_permission", awaitingPermission)
        super.onSaveInstanceState(state)
    }
    override fun onDestroy() {
        if (AppUpdates.enabled(this)) feed.removeObserver(observer)
        scope.cancel(); super.onDestroy()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(body: LinearLayout, value: String, size: Float = 14f) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.rgb(222, 255, 249)); typeface = Typeface.MONOSPACE
        setPadding(0, dp(10), 0, dp(10)); body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun button(body: LinearLayout, value: String, viewId: Int, action: () -> Unit) = Button(this).apply {
        id = viewId; text = value; isAllCaps = false; minHeight = dp(48)
        body.addView(this, LinearLayout.LayoutParams(-1, -2)); setOnClickListener { action() }
    }
}
