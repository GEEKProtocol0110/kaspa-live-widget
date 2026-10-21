package com.kaspa.livewidget.update

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.security.MessageDigest

class UpdateSecurityTest {
    private val payload = "a bounded, publisher-verified update".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it.toInt() and 255) }
    private val release = AppRelease("1.7.0", AppRelease.url("1.7.0"), hash, payload.size.toLong())
    private fun published(version: String = "1.7.0") = """{"tag_name":"v$version","draft":false,"prerelease":false,"assets":[{"name":"kaspa-live-widget.apk","state":"uploaded","size":${payload.size},"digest":"sha256:$hash","browser_download_url":"${AppRelease.url(version)}"}]}"""

    @Test fun stableReleasesUseNumericVersionsAndExactPublisherUrls() {
        assertEquals(release, AppRelease.published(published()))
        assertTrue(AppVersion.parse("1.10.0")!! > AppVersion.parse("1.9.9")!!)
        assertTrue(release.newerThan("1.6.0"))
        assertFalse(release.newerThan("1.7.0"))
        assertFalse(release.newerThan("broken"))
        for (value in listOf("1.7.0-beta", "01.7.0", "1.7", "9999999.1.1", "-1.2.3")) {
            assertNull(AppVersion.parse(value))
        }
        for (bad in listOf(
            published().replace("\"draft\":false", "\"draft\":true"),
            published().replace("\"prerelease\":false", "\"prerelease\":true"),
            published().replace("sha256:$hash", "sha256:bad"),
            published().replace("\"size\":${payload.size}", "\"size\":999999999"),
            published().replace("github.com/", "github.com.evil.example/"),
            published().replace("/GEEKProtocol0110/", "/another-publisher/"),
            published().replace("\"state\":\"uploaded\"", "\"state\":\"new\""),
            published().replace("kaspa-live-widget.apk", "something-else.apk"),
            published("1.7.0-beta"), "not json"
        )) assertNull(bad, AppRelease.published(bad))
    }

    @Test fun updatesMustKeepPackageAndSignerAndAdvanceBothVersionValues() {
        val installed = ApkIdentity(AppRelease.PACKAGE, "1.6.0", 12, setOf("publisher-cert"))
        val next = ApkIdentity(AppRelease.PACKAGE, "1.7.0", 13, setOf("publisher-cert"))
        assertTrue(installed.accepts(next, release))
        assertFalse(installed.accepts(next.copy(packageName = "another.app"), release))
        assertFalse(installed.accepts(next.copy(signers = setOf("attacker-cert")), release))
        assertFalse(installed.accepts(next.copy(signers = emptySet()), release))
        assertFalse(installed.accepts(next.copy(versionCode = 12), release))
        assertFalse(installed.accepts(next.copy(versionCode = 11), release))
        assertFalse(installed.accepts(next.copy(versionName = "1.8.0"), release))
        val downgrade = release.copy(versionName = "1.5.0", downloadUrl = AppRelease.url("1.5.0"))
        assertFalse(installed.accepts(next.copy(versionName = "1.5.0"), downgrade))
        assertFalse(installed.copy(signers = emptySet()).accepts(next, release))
    }

    private fun source(bytes: ByteArray = payload, code: Int = 200) = UpdateSource(OkHttpClient.Builder()
        .addInterceptor { chain ->
            assertEquals("https", chain.request().url.scheme)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code)
                .message("fixture").body(bytes.toResponseBody()).build()
        }.build())

    @Test fun downloadProgressSurvivesUiIndependentlyAndCorruptOrTruncatedFilesNeverBecomeReady() = runBlocking {
        val dir = Files.createTempDirectory("widget-update").toFile()
        val file = java.io.File(dir, "update.apk")
        try {
            val progress = mutableListOf<Int>()
            source().download(release, file) { progress += it }
            assertArrayEquals(payload, file.readBytes())
            assertEquals(100, progress.last())
            file.delete()
            for (bytes in listOf(payload.dropLast(1).toByteArray(), ByteArray(payload.size) { 7 }, payload + 1)) {
                var rejected = false
                try { source(bytes).download(release, file) { } } catch (_: Exception) { rejected = true }
                assertTrue("Invalid update was accepted", rejected)
                assertFalse(file.exists())
                assertFalse(java.io.File(dir, "download.partial").exists())
            }
            var rejected = false
            try { source(code = 404).download(release, file) { } } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            assertFalse(file.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun metadataFailureIsNeverReportedAsAnUpdate() = runBlocking {
        assertEquals(release, source(published().toByteArray()).latest())
        assertNull(source(code = 429).latest())
        assertNull(source("invalid".toByteArray()).latest())
        assertNull(source(ByteArray(512 * 1024 + 1)).latest())
    }
}
