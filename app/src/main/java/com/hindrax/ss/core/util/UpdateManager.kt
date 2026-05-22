package com.hindrax.ss.core.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
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
                        return@withContext UpdateResult.Error("GITHUB_HTTP_${response.code}")
                    }
                    
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
                        clearCachedUpdate()
                        return@withContext UpdateResult.NoUpdate("GITHUB_RELEASE_NOT_PUBLIC")
                    }

                    val latestTag = normalizeVersion(json.getString("tag_name"))
                    val releasePageUrl = json.optString("html_url")
                    val releaseName = json.optString("name", "Release v$latestTag")
                    val publishedAt = json.optString("published_at")
                    
                    val apkAsset = json.getJSONArray("assets").let { assets ->
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk", ignoreCase = true)) return@let asset
                        }
                        null
                    }

                    val downloadUrl = apkAsset?.getString("browser_download_url")

                    if (isNewerVersion(currentVersion, latestTag) && downloadUrl != null) {
                        UpdateResult.Available(
                            info = UpdateInfo(
                                version = latestTag,
                                url = downloadUrl,
                                releaseName = releaseName,
                                releasePageUrl = releasePageUrl,
                                publishedAt = publishedAt,
                                assetName = apkAsset.optString("name", "hindrax-v$latestTag.apk")
                            )
                        ).also { cacheUpdate(it.info) }
                    } else {
                        clearCachedUpdate()
                        if (downloadUrl == null) {
                            UpdateResult.NoUpdate("GITHUB_RELEASE_WITHOUT_APK: latest=v$latestTag")
                        } else {
                            UpdateResult.NoUpdate(
                                "LATEST_RELEASE_NOT_NEWER: current=v${normalizeVersion(currentVersion)} latest=v$latestTag asset=${apkAsset.optString("name", "apk")}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                UpdateResult.Error(e.message ?: "Network error")
            }
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

    fun downloadAndInstall(info: UpdateInfo) {
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

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    if (isDownloadSuccessful(downloadManager, downloadId) && destination.exists()) {
                        installApk(destination)
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

    private fun receiverFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
