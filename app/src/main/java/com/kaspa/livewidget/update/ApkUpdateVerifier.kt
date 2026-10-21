package com.kaspa.livewidget.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

internal data class ApkIdentity(val packageName: String, val versionName: String?, val versionCode: Long, val signers: Set<String>) {
    fun accepts(candidate: ApkIdentity, release: AppRelease) =
        packageName == AppRelease.PACKAGE && candidate.packageName == packageName &&
            candidate.versionName == release.versionName && candidate.versionCode > versionCode &&
            release.newerThan(versionName ?: "") &&
            signers.isNotEmpty() && candidate.signers == signers
}

internal object ApkUpdateVerifier {
    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo): ApkIdentity {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val signers = signatures?.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        }?.toSet() ?: emptySet()
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return ApkIdentity(info.packageName, info.versionName, code, signers)
    }

    @Suppress("DEPRECATION")
    fun verify(context: Context, file: File, release: AppRelease) {
        require(release.valid() && file.isFile && file.length() == release.size) { "Update file is incomplete" }
        require(AppRelease.digest(file) == release.sha256) { "Update checksum did not match" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val pm = context.packageManager
        val installed = identity(pm.getPackageInfo(context.packageName, flags))
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Update is not a valid APK")
        require(installed.accepts(identity(archive), release)) { "Update must be a newer version signed by the existing app's publisher" }
    }
}
