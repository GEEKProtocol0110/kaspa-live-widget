package com.kaspa.livewidget.update

import com.google.gson.JsonParser
import org.json.JSONObject
import java.security.MessageDigest
import java.io.File

internal data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion) = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    companion object {
        fun parse(value: String): AppVersion? {
            if (!Regex("(?:0|[1-9][0-9]{0,5})\\.(?:0|[1-9][0-9]{0,5})\\.(?:0|[1-9][0-9]{0,5})").matches(value)) return null
            val parts = value.split('.').map { it.toInt() }
            return AppVersion(parts[0], parts[1], parts[2])
        }
    }
}

internal data class AppRelease(val versionName: String, val downloadUrl: String, val sha256: String, val size: Long) {
    fun valid() = AppVersion.parse(versionName) != null && size in 1..MAX_APK_BYTES &&
        Regex("[a-f0-9]{64}").matches(sha256) && downloadUrl == url(versionName)
    fun newerThan(installed: String): Boolean {
        val current = AppVersion.parse(installed) ?: return false
        return valid() && AppVersion.parse(versionName)!! > current
    }
    fun json() = JSONObject().put("version", versionName).put("url", downloadUrl)
        .put("sha256", sha256).put("size", size).toString()

    companion object {
        const val PACKAGE = "com.kaspa.livewidget"
        const val MAX_APK_BYTES = 30L * 1024 * 1024
        const val RELEASES = "https://github.com/GEEKProtocol0110/kaspa-live-widget/releases"
        const val API = "https://api.github.com/repos/GEEKProtocol0110/kaspa-live-widget/releases/latest"
        fun url(version: String) = "$RELEASES/download/v$version/kaspa-live-widget.apk"

        fun cached(json: String?): AppRelease? = try {
            if (json == null || json.length > 4096) null else JSONObject(json).let {
                AppRelease(it.getString("version"), it.getString("url"), it.getString("sha256"), it.getLong("size"))
                    .takeIf { release -> release.valid() }
            }
        } catch (_: Exception) { null }

        fun published(json: String): AppRelease? = try {
            if (json.length > 512 * 1024) null else {
                val root = JsonParser.parseString(json).asJsonObject
                require(!root.get("draft").asBoolean && !root.get("prerelease").asBoolean)
                val tag = root.get("tag_name").asString
                require(tag.startsWith("v"))
                val version = tag.removePrefix("v")
                require(AppVersion.parse(version) != null)
                val asset = root.getAsJsonArray("assets").filter {
                    it.asJsonObject.get("name")?.asString == "kaspa-live-widget.apk"
                }.single().asJsonObject
                require(asset.get("state").asString == "uploaded")
                val digest = asset.get("digest").asString
                require(digest.startsWith("sha256:"))
                AppRelease(version, asset.get("browser_download_url").asString,
                    digest.removePrefix("sha256:"), asset.get("size").asLong).takeIf { it.valid() }
            }
        } catch (_: Exception) { null }

        fun digest(file: File): String {
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val bytes = ByteArray(32 * 1024)
                while (true) { val count = input.read(bytes); if (count < 0) break; hash.update(bytes, 0, count) }
            }
            return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        }
    }
}
