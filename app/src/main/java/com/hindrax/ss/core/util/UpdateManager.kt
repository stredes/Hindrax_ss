package com.hindrax.ss.core.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val prefs = context.getSharedPreferences("hindrax_update_prefs", Context.MODE_PRIVATE)

    // Public GitHub release channel. release.sh pushes the tag; GitHub Actions attaches the APK.
    private val githubRepo = "stredes/Hindrax_ss"
    private val latestReleaseUrl = "https://api.github.com/repos/$githubRepo/releases/latest"
    private val releasesUrl = "https://api.github.com/repos/$githubRepo/releases?per_page=10"
    private val latestReleaseWebUrl = "https://github.com/$githubRepo/releases/latest"

    suspend fun checkForUpdates(currentVersion: String): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(latestReleaseUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Hindrax-SS-Updater")
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 404) {
                            clearCachedUpdate()
                            return@withContext UpdateResult.NoUpdate("GITHUB_RELEASE_NOT_PUBLISHED")
                        }
                        if (response.code == 403) {
                            return@withContext checkForUpdatesFromGithubWeb(currentVersion, "GITHUB_API_RATE_LIMIT")
                        }
                        return@withContext UpdateResult.Error("GITHUB_HTTP_${response.code}")
                    }

                    val json = JSONObject(response.body?.string() ?: "")
                    val update = updateInfoFromRelease(json, currentVersion)
                        ?: findUpdateInRecentReleases(currentVersion)

                    if (update != null) {
                        UpdateResult.Available(update).also { cacheUpdate(it.info) }
                    } else {
                        clearCachedUpdate()
                        val latestTag = normalizeVersion(json.optString("tag_name", "0"))
                        UpdateResult.NoUpdate(
                            "NO_RELEASE_APK_NEWER_THAN_CURRENT: current=v${normalizeVersion(currentVersion)} latest=v$latestTag"
                        )
                    }
                }
            } catch (e: Exception) {
                UpdateResult.Error(e.message ?: "Network error")
            }
        }
    }

    private fun findUpdateInRecentReleases(currentVersion: String): UpdateInfo? {
        val request = Request.Builder()
            .url(releasesUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "Hindrax-SS-Updater")
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val releases = org.json.JSONArray(response.body?.string() ?: "[]")
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val info = updateInfoFromRelease(release, currentVersion)
                if (info != null) return info
            }
            null
        }
    }

    private fun updateInfoFromRelease(release: JSONObject, currentVersion: String): UpdateInfo? {
        return GithubReleaseUpdateParser.updateInfoFromRelease(release, currentVersion, githubRepo)
    }

    private fun checkForUpdatesFromGithubWeb(currentVersion: String, reason: String): UpdateResult {
        return try {
            val request = Request.Builder()
                .url(latestReleaseWebUrl)
                .header("User-Agent", "Hindrax-SS-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return UpdateResult.Error("$reason; GITHUB_WEB_HTTP_${response.code}")
                }

                val latestTagWithPrefix = response.request.url.encodedPathSegments
                    .let { segments ->
                        val tagIndex = segments.indexOf("tag")
                        if (tagIndex >= 0) segments.getOrNull(tagIndex + 1) else null
                    }
                    ?.takeIf { it.startsWith("v", ignoreCase = true) }
                    ?: return UpdateResult.NoUpdate("$reason; GITHUB_LATEST_TAG_NOT_RESOLVED")

                val latestVersion = normalizeVersion(latestTagWithPrefix)
                val assetName = "hindrax-$latestTagWithPrefix.apk"
                val downloadUrl = "https://github.com/$githubRepo/releases/latest/download/$assetName"

                if (isNewerVersion(currentVersion, latestVersion)) {
                    UpdateResult.Available(
                        info = UpdateInfo(
                            version = latestVersion,
                            url = downloadUrl,
                            releaseName = "Release $latestTagWithPrefix",
                            releasePageUrl = "https://github.com/$githubRepo/releases/tag/$latestTagWithPrefix",
                            publishedAt = "",
                            assetName = assetName
                        )
                    ).also { cacheUpdate(it.info) }
                } else {
                    clearCachedUpdate()
                    UpdateResult.NoUpdate(
                        "$reason; LATEST_RELEASE_NOT_NEWER: current=v${normalizeVersion(currentVersion)} latest=v$latestVersion asset=$assetName"
                    )
                }
            }
        } catch (e: Exception) {
            UpdateResult.Error("$reason; ${e.message ?: "GitHub web fallback error"}")
        }
    }

    fun getCachedUpdate(currentVersion: String): UpdateResult.Available? {
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        val url = prefs.getString(KEY_URL, null) ?: return null
        if (!isNewerVersion(currentVersion, version)) {
            clearCachedUpdate()
            return null
        }
        return UpdateResult.Available(
            UpdateInfo(
                version = version,
                url = url,
                releaseName = prefs.getString(KEY_RELEASE_NAME, "Release v$version") ?: "Release v$version",
                releasePageUrl = prefs.getString(KEY_RELEASE_PAGE, "") ?: "",
                publishedAt = prefs.getString(KEY_PUBLISHED_AT, "") ?: "",
                assetName = prefs.getString(KEY_ASSET_NAME, "hindrax-v$version.apk") ?: "hindrax-v$version.apk"
            )
        )
    }

    private fun cacheUpdate(info: UpdateInfo) {
        prefs.edit()
            .putString(KEY_VERSION, info.version)
            .putString(KEY_URL, info.url)
            .putString(KEY_RELEASE_NAME, info.releaseName)
            .putString(KEY_RELEASE_PAGE, info.releasePageUrl)
            .putString(KEY_PUBLISHED_AT, info.publishedAt)
            .putString(KEY_ASSET_NAME, info.assetName)
            .apply()
    }

    private fun clearCachedUpdate() {
        prefs.edit()
            .remove(KEY_VERSION)
            .remove(KEY_URL)
            .remove(KEY_RELEASE_NAME)
            .remove(KEY_RELEASE_PAGE)
            .remove(KEY_PUBLISHED_AT)
            .remove(KEY_ASSET_NAME)
            .apply()
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = normalizeVersion(current).split(".").map { it.toIntOrNull() ?: 0 }
        val lateParts = normalizeVersion(latest).split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(currParts.size, lateParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    private fun normalizeVersion(value: String): String {
        return value.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .filter { it.isDigit() || it == '.' }
            .ifBlank { "0" }
    }

    fun downloadAndInstall(info: UpdateInfo, onStatus: (String) -> Unit = {}) {
        val destination = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            info.assetName.ifBlank { "hindrax-update-v${info.version}.apk" }
        )
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(info.url))
            .setTitle("Hindrax v${info.version}")
            .setDescription("Descargando actualización publicada en GitHub Releases...")
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        onStatus("DOWNLOAD_QUEUED: ${info.assetName}")

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    if (isDownloadSuccessful(downloadManager, downloadId) && destination.exists()) {
                        val preflight = validateDownloadedApk(destination)
                        if (preflight != "APK_PREFLIGHT_OK") {
                            onStatus(preflight)
                        } else {
                            onStatus(installApk(destination))
                        }
                    } else {
                        onStatus(downloadFailureStatus(downloadManager, downloadId))
                    }
                    context.unregisterReceiver(this)
                }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), receiverFlag())
    }

    private fun isDownloadSuccessful(downloadManager: DownloadManager, downloadId: Long): Boolean {
        return downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            cursor != null &&
                cursor.moveToFirst() &&
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
    }

    private fun downloadFailureStatus(downloadManager: DownloadManager, downloadId: Long): String {
        return downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                "DOWNLOAD_FAILED_REASON_$reason"
            } else {
                "DOWNLOAD_FAILED_NO_CURSOR"
            }
        }
    }

    private fun receiverFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }
    }

    private fun validateDownloadedApk(file: File): String {
        val archiveInfo = archivePackageInfo(file)
            ?: return "APK_PREFLIGHT_ERROR: PACKAGE_INFO_UNREADABLE"
        if (archiveInfo.packageName != context.packageName) {
            return "APK_PREFLIGHT_ERROR: PACKAGE_MISMATCH ${archiveInfo.packageName} != ${context.packageName}"
        }

        val installedInfo = packageInfo(context.packageName)
            ?: return "APK_PREFLIGHT_ERROR: INSTALLED_PACKAGE_UNREADABLE"
        if (versionCodeOf(archiveInfo) <= versionCodeOf(installedInfo)) {
            return "APK_PREFLIGHT_ERROR: VERSION_NOT_NEWER apk=${versionCodeOf(archiveInfo)} installed=${versionCodeOf(installedInfo)}"
        }

        val installedDigests = signatureDigests(installedInfo)
        val archiveDigests = signatureDigests(archiveInfo)
        if (installedDigests.isNotEmpty() && archiveDigests.isNotEmpty() && installedDigests != archiveDigests) {
            return "APK_PREFLIGHT_ERROR: SIGNATURE_MISMATCH installed=${installedDigests.shortLabel()} apk=${archiveDigests.shortLabel()} uninstall_incompatible_build_then_install_release"
        }

        return "APK_PREFLIGHT_OK"
    }

    private fun archivePackageInfo(file: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
    }

    private fun packageInfo(packageName: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, flags)
        }.getOrNull()
    }

    private fun versionCodeOf(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun signatureDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures.orEmpty()
        }
        return signatures.map(::signatureDigest).toSet()
    }

    private fun signatureDigest(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun Set<String>.shortLabel(): String {
        return sorted().joinToString("+") { digest ->
            digest.take(8)
        }
    }

    private fun installApk(file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return "INSTALL_PERMISSION_REQUIRED: enable_unknown_sources_for_hindrax_and_retry"
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(intent)
        return "INSTALLER_LAUNCHED"
    }

    private companion object {
        const val KEY_VERSION = "available_version"
        const val KEY_URL = "available_url"
        const val KEY_RELEASE_NAME = "release_name"
        const val KEY_RELEASE_PAGE = "release_page_url"
        const val KEY_PUBLISHED_AT = "published_at"
        const val KEY_ASSET_NAME = "asset_name"
    }
}

internal object GithubReleaseUpdateParser {
    fun updateInfoFromRelease(release: JSONObject, currentVersion: String, githubRepo: String): UpdateInfo? {
        if (release.optBoolean("draft", false) || release.optBoolean("prerelease", false)) return null

        val latestTagRaw = release.optString("tag_name")
        val latestVersion = normalizeVersion(latestTagRaw)
        if (!isNewerVersion(currentVersion, latestVersion)) return null

        val normalizedTag = latestTagRaw
            .takeIf { it.isNotBlank() }
            ?: "v$latestVersion"
        val fallbackAssetName = "hindrax-$normalizedTag.apk"
        val apkAsset = bestApkAsset(release, normalizedTag)
        val assetName = apkAsset?.optString("name")?.takeIf { it.isNotBlank() } ?: fallbackAssetName
        val downloadUrl = apkAsset
            ?.optString("browser_download_url")
            ?.takeIf { it.isNotBlank() }
            ?: "https://github.com/$githubRepo/releases/download/$normalizedTag/$fallbackAssetName"

        return UpdateInfo(
            version = latestVersion,
            url = downloadUrl,
            releaseName = release.optString("name", "Release v$latestVersion"),
            releasePageUrl = release.optString("html_url", "https://github.com/$githubRepo/releases/tag/$normalizedTag"),
            publishedAt = release.optString("published_at"),
            assetName = assetName
        )
    }

    private fun bestApkAsset(release: JSONObject, normalizedTag: String): JSONObject? {
        val assets = release.optJSONArray("assets") ?: return null
        var bestAsset: JSONObject? = null
        var bestScore = Int.MIN_VALUE

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            var score = 10
            if (name.contains("hindrax", ignoreCase = true)) score += 20
            if (name.contains(normalizedTag, ignoreCase = true)) score += 20
            if (!name.contains("debug", ignoreCase = true)) score += 10
            if (name.equals("hindrax-$normalizedTag.apk", ignoreCase = true)) score += 30

            if (score > bestScore) {
                bestScore = score
                bestAsset = asset
            }
        }

        return bestAsset
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = normalizeVersion(current).split(".").map { it.toIntOrNull() ?: 0 }
        val lateParts = normalizeVersion(latest).split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(currParts.size, lateParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    private fun normalizeVersion(value: String): String {
        return value.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .filter { it.isDigit() || it == '.' }
            .ifBlank { "0" }
    }
}

data class UpdateInfo(
    val version: String,
    val url: String,
    val releaseName: String,
    val releasePageUrl: String,
    val publishedAt: String,
    val assetName: String
)

sealed class UpdateResult {
    data class NoUpdate(val reason: String = "APP_UP_TO_DATE") : UpdateResult()
    data class Available(val info: UpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}
