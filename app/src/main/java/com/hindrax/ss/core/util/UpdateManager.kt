package com.hindrax.ss.core.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
    private val GITHUB_REPO = "gian/Hindrax_ss" // REEMPLAZAR CON TU REPO
    private val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdates(currentVersion: String): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext UpdateResult.NoUpdate
                    
                    val json = JSONObject(response.body?.string() ?: "")
                    val latestTag = json.getString("tag_name").removePrefix("v")
                    
                    val apkAsset = json.getJSONArray("assets").let { assets ->
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.getString("name").endsWith(".apk")) return@let asset
                        }
                        null
                    }

                    val downloadUrl = apkAsset?.getString("browser_download_url")

                    if (isNewerVersion(currentVersion, latestTag) && downloadUrl != null) {
                        UpdateResult.Available(latestTag, downloadUrl)
                    } else {
                        UpdateResult.NoUpdate
                    }
                }
            } catch (e: Exception) {
                UpdateResult.Error(e.message ?: "Network error")
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val lateParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(currParts.size, lateParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    fun downloadAndInstall(url: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Hindrax Security Update")
            .setDescription("Descargando v${url.substringAfterLast("/")}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
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
}

sealed class UpdateResult {
    object NoUpdate : UpdateResult()
    data class Available(val version: String, val url: String) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}
