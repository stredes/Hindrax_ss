package com.hindrax.ss.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class ApiHindraxSyncResponse(
    val items: JSONArray,
    val serverTime: Long
)

@Singleton
class ApiHindraxClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val configStore: ApiHindraxConfigStore
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun syncTasks(items: JSONArray): ApiHindraxSyncResponse? {
        return postSync("/api/v1/tasks/sync", items)
    }

    suspend fun syncInventory(items: JSONArray): ApiHindraxSyncResponse? {
        return postSync("/api/v1/inventory/sync", items)
    }

    suspend fun heartbeat(deviceId: String, nickname: String?, appVersion: String?): Boolean {
        val config = configStore.load()
        if (!config.isReady) return false
        val body = JSONObject().apply {
            put("deviceId", deviceId)
            if (!nickname.isNullOrBlank()) put("nickname", nickname)
            if (!appVersion.isNullOrBlank()) put("appVersion", appVersion)
            put("updatedAt", System.currentTimeMillis())
        }
        val response = executeJsonPost(config, "/api/v1/devices/heartbeat", body)
        return response != null
    }

    private suspend fun postSync(path: String, items: JSONArray): ApiHindraxSyncResponse? {
        val config = configStore.load()
        if (!config.isReady) return null
        val body = JSONObject().put("items", items)
        val response = executeJsonPost(config, path, body) ?: return null
        return ApiHindraxSyncResponse(
            items = response.optJSONArray("items") ?: JSONArray(),
            serverTime = response.optLong("serverTime", System.currentTimeMillis())
        )
    }

    private suspend fun executeJsonPost(
        config: ApiHindraxConfig,
        path: String,
        body: JSONObject
    ): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.baseUrl}$path")
            .header("Authorization", "Bearer ${config.token}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val payload = response.body?.string().orEmpty()
            if (payload.isBlank()) null else JSONObject(payload)
        }
    }
}
