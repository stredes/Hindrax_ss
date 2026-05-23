package com.hindrax.ss.data.remote

import android.content.Context
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ApiHindraxRemoteSyncResult(
    val enabled: Boolean,
    val pushedTasks: Int = 0,
    val pulledTasks: Int = 0,
    val pushedInventory: Int = 0,
    val pulledInventory: Int = 0,
    val heartbeatSent: Boolean = false
)

@Singleton
class ApiHindraxRemoteSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val deviceIdManager: DeviceIdManager,
    private val client: ApiHindraxClient,
    private val configStore: ApiHindraxConfigStore
) {
    private val myDeviceId = deviceIdManager.getDeviceId()

    suspend fun syncAll(): ApiHindraxRemoteSyncResult {
        if (!configStore.load().isReady) return ApiHindraxRemoteSyncResult(enabled = false)

        return coroutineScope {
            val tasksJob = async { syncTasks() }
            val inventoryJob = async { syncInventory() }
            val heartbeatJob = async {
                client.heartbeat(
                    deviceId = myDeviceId,
                    nickname = deviceIdManager.getNickname(),
                    appVersion = appVersionName()
                )
            }
            val tasks = tasksJob.await()
            val inventory = inventoryJob.await()
            ApiHindraxRemoteSyncResult(
                enabled = true,
                pushedTasks = tasks.first,
                pulledTasks = tasks.second,
                pushedInventory = inventory.first,
                pulledInventory = inventory.second,
                heartbeatSent = heartbeatJob.await()
            )
        }
    }

    suspend fun pushTask(task: TaskEntity) {
        if (!configStore.load().isReady) return
        client.syncTasks(JSONArray().put(task.toApiJson()))
    }

    suspend fun pushInventory(item: InventoryEntity) {
        if (!configStore.load().isReady) return
        client.syncInventory(JSONArray().put(item.toApiJson()))
    }

    private suspend fun syncTasks(): Pair<Int, Int> {
        val local = taskDao.getAllTasksSync()
        val response = client.syncTasks(JSONArray().also { array ->
            local.forEach { array.put(it.toApiJson()) }
        }) ?: return local.size to 0
        val applied = applyRemoteTasks(response.items)
        return local.size to applied
    }

    private suspend fun syncInventory(): Pair<Int, Int> {
        val local = inventoryDao.getAllInventorySync()
        val response = client.syncInventory(JSONArray().also { array ->
            local.forEach { array.put(it.toApiJson()) }
        }) ?: return local.size to 0
        val applied = applyRemoteInventory(response.items)
        return local.size to applied
    }

    private suspend fun applyRemoteTasks(items: JSONArray): Int {
        var applied = 0
        for (index in 0 until items.length()) {
            val json = items.optJSONObject(index) ?: continue
            if (json.optString("deviceId") == myDeviceId) continue
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: continue
            val incomingUpdatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            val existing = taskDao.getByTitle(title) ?: taskDao.getByTitle("[REMOTE] $title")
            if (existing != null && existing.updatedAt >= incomingUpdatedAt) continue

            val incoming = TaskEntity(
                id = existing?.id ?: 0,
                title = title,
                description = json.optString("description", ""),
                status = json.optEnum("status", TaskStatus.PENDIENTE),
                type = json.optEnum("type", TaskType.GENERAL),
                scheduledTime = json.optNullableLong("scheduledTime"),
                locationName = json.optString("locationName", "").takeIf { it.isNotBlank() },
                latitude = json.optNullableDouble("latitude"),
                longitude = json.optNullableDouble("longitude"),
                quantity = json.optNullableDouble("quantity"),
                unit = json.optString("unit", "").takeIf { it.isNotBlank() },
                inventoryItemId = null,
                assignedPeerId = json.optString("assignedPeerId", "").takeIf { it.isNotBlank() },
                checklist = json.optChecklist(),
                createdAt = existing?.createdAt ?: incomingUpdatedAt,
                updatedAt = incomingUpdatedAt,
                isDeleted = json.optBoolean("deleted", false)
            )
            if (existing == null) taskDao.insert(incoming) else taskDao.update(incoming)
            applied++
        }
        return applied
    }

    private suspend fun applyRemoteInventory(items: JSONArray): Int {
        var applied = 0
        for (index in 0 until items.length()) {
            val json = items.optJSONObject(index) ?: continue
            if (json.optString("deviceId") == myDeviceId) continue
            val name = json.optString("name").takeIf { it.isNotBlank() } ?: continue
            val incomingUpdatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            val existing = inventoryDao.getByName(name)
            if (existing != null && existing.updatedAt >= incomingUpdatedAt) continue

            val incoming = InventoryEntity(
                id = existing?.id ?: 0,
                name = name,
                category = json.optString("category", "REMOTE"),
                currentQuantity = json.optDouble("quantity", 0.0),
                minQuantity = json.optDouble("minQuantity", existing?.minQuantity ?: 0.0),
                unit = json.optString("unit", existing?.unit ?: "u"),
                updatedAt = incomingUpdatedAt
            )
            inventoryDao.insert(incoming)
            applied++
        }
        return applied
    }

    private fun TaskEntity.toApiJson(): JSONObject {
        return JSONObject().apply {
            put("id", "$myDeviceId-task-$id")
            put("deviceId", myDeviceId)
            put("title", title)
            put("description", description)
            put("status", status.name)
            put("type", type.name)
            if (scheduledTime != null) put("scheduledTime", scheduledTime)
            if (locationName != null) put("locationName", locationName)
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
            if (quantity != null) put("quantity", quantity)
            if (unit != null) put("unit", unit)
            if (inventoryItemId != null) put("inventoryItemId", inventoryItemId)
            if (assignedPeerId != null) put("assignedPeerId", assignedPeerId)
            put("checklist", JSONArray().also { array ->
                checklist.forEach { item ->
                    array.put(JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text)
                        put("checked", item.isChecked)
                        if (item.quantity != null) put("q", item.quantity)
                        if (item.unit != null) put("u", item.unit)
                    })
                }
            })
            put("deleted", isDeleted)
            put("updatedAt", updatedAt)
        }
    }

    private fun InventoryEntity.toApiJson(): JSONObject {
        return JSONObject().apply {
            put("id", "$myDeviceId-inventory-$id")
            put("deviceId", myDeviceId)
            put("name", name)
            put("category", category)
            put("quantity", currentQuantity)
            put("minQuantity", minQuantity)
            put("unit", unit)
            put("updatedAt", updatedAt)
        }
    }

    private fun appVersionName(): String? {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName
        }.getOrNull()
    }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    return if (has(key) && !isNull(key)) optLong(key) else null
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    return if (has(key) && !isNull(key)) optDouble(key) else null
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
    val value = optString(key, "")
    return enumValues<T>().firstOrNull { it.name == value } ?: fallback
}

private fun JSONObject.optChecklist(): List<ChecklistItem> {
    val array = optJSONArray("checklist") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            add(
                ChecklistItem(
                    id = item.optString("id", UUID.randomUUID().toString()),
                    text = item.optString("text"),
                    isChecked = item.optBoolean("checked", false),
                    quantity = item.optNullableDouble("q"),
                    unit = item.optString("u", "").takeIf { it.isNotBlank() }
                )
            )
        }
    }
}
