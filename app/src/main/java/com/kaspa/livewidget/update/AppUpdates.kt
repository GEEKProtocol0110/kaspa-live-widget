package com.kaspa.livewidget.update

import android.content.Context
import com.kaspa.livewidget.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class UpdateStore(context: Context) {
    private val prefs = context.getSharedPreferences("kaspa_app_updates", Context.MODE_PRIVATE)
    val automatic get() = prefs.getBoolean("automatic", true)
    fun automatic(value: Boolean) { prefs.edit().putBoolean("automatic", value).apply() }
    val lastAttempt get() = prefs.getLong("attempt", 0L)
    val lastSuccess get() = prefs.getLong("success", 0L)
    val release get() = AppRelease.cached(prefs.getString("release", null))
    val ready get() = AppRelease.cached(prefs.getString("ready", null))
    fun available() = release?.takeIf { it.newerThan(BuildConfig.VERSION_NAME) }
    fun checked(release: AppRelease?, now: Long) {
        prefs.edit().putLong("attempt", now).apply {
            if (release != null) putLong("success", now).putString("release", release.json())
        }.apply()
    }
    fun ready(release: AppRelease?) { prefs.edit().putString("ready", release?.json()).apply() }
}

/** Only the public release app uses this channel. Play bundles have it compiled out. */
internal object AppUpdates {
    private val checkMutex = Mutex()
    fun enabled(context: Context) = BuildConfig.DIRECT_UPDATES && context.packageName == AppRelease.PACKAGE
    fun directory(context: Context) = File(context.cacheDir, "app-updates").apply { mkdirs() }
    fun apk(context: Context) = File(directory(context), "kaspa-live-widget.apk")

    suspend fun check(context: Context, force: Boolean = false) = checkMutex.withLock {
        if (!enabled(context)) return@withLock
        val store = UpdateStore(context)
        val now = System.currentTimeMillis()
        if (!force && now - store.lastAttempt in 0 until TimeUnit.DAYS.toMillis(1)) return@withLock
        val release = try { UpdateSource().latest() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        store.checked(release, now)
    }
}

internal class UpdateSource(private val client: OkHttpClient = client()) {
    companion object {
        private val hosts = setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
        fun client() = OkHttpClient.Builder().callTimeout(2, TimeUnit.MINUTES)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .followSslRedirects(false).addNetworkInterceptor { chain ->
                val url = chain.request().url
                if (url.scheme != "https" || url.host !in hosts) throw IOException("Unexpected update source")
                chain.proceed(chain.request())
            }.build()
    }

    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(AppRelease.API).header("Accept", "application/vnd.github+json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            if (body.contentLength() > 512 * 1024) return@withContext null
            // Bound chunked responses as well as responses with Content-Length.
            val bytes = body.byteStream().use { it.readBytesBounded(512 * 1024L) }
            AppRelease.published(bytes.toString(Charsets.UTF_8))
        }
    }

    suspend fun download(release: AppRelease, target: File, progress: suspend (Int) -> Unit) = withContext(Dispatchers.IO) {
        require(release.valid())
        val partial = File(target.parentFile, "download.partial")
        target.parentFile?.mkdirs()
        try {
            client.newCall(Request.Builder().url(release.downloadUrl).build()).execute().use { response ->
                require(response.isSuccessful) { "Update download is unavailable" }
                val body = response.body ?: error("Empty update download")
                require(body.contentLength() < 0 || body.contentLength() == release.size) { "Update file size did not match" }
                body.byteStream().buffered().use { input -> partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(32 * 1024); var total = 0L; var shown = -1
                    while (true) {
                        val count = input.read(buffer); if (count < 0) break
                        total += count
                        require(total <= release.size && total <= AppRelease.MAX_APK_BYTES) { "Update download was too large" }
                        output.write(buffer, 0, count)
                        val percent = (total * 100 / release.size).toInt()
                        if (percent != shown) { progress(percent); shown = percent }
                    }
                    require(total == release.size) { "Update download was interrupted" }
                } }
            }
            require(AppRelease.digest(partial) == release.sha256) { "Update checksum did not match" }
            require(partial.renameTo(target)) { "Could not save the update" }
        } finally { partial.delete() }
    }

    private fun java.io.InputStream.readBytesBounded(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192); var total = 0L
        while (true) {
            val count = read(buffer); if (count < 0) break
            total += count; require(total <= limit) { "Update metadata was too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
