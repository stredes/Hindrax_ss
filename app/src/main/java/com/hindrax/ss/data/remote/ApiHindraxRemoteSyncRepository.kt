package com.hindrax.ss.data.remote

import android.content.Context
import android.util.Log
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.data.db.ChatDao
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.entity.MessageStatus
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.data.entity.TaskHistoryEntity
import com.hindrax.ss.domain.inventory.InventoryApplicationKey
import com.hindrax.ss.domain.inventory.ProductNameNormalizer
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
    val bootstrapUploaded: Boolean = false,
    val pushedTasks: Int = 0,
    val pulledTasks: Int = 0,
    val pushedInventory: Int = 0,
    val pulledInventory: Int = 0,
    val pushedChat: Int = 0,
    val pulledChat: Int = 0,
    val heartbeatSent: Boolean = false
)

private data class RemoteCollectionSyncResult(
    val success: Boolean,
    val pushed: Int = 0,
    val pulled: Int = 0
)

@Singleton
class ApiHindraxRemoteSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val deviceIdManager: DeviceIdManager,
    private val client: ApiHindraxClient,
    private val configStore: ApiHindraxConfigStore
) {
    private val myDeviceId = deviceIdManager.getDeviceId()

    suspend fun syncAll(): ApiHindraxRemoteSyncResult {
        val config = configStore.load()
        if (!config.isReady) return ApiHindraxRemoteSyncResult(enabled = false)
        val bootstrapPending = configStore.shouldRunBootstrap(config)

        return coroutineScope {
            val tasksJob = async { syncTasks() }
            val inventoryJob = async { syncInventory() }
            val chatJob = async { syncChat() }
            val heartbeatJob = async {
                client.heartbeat(
                    deviceId = myDeviceId,
                    nickname = deviceIdManager.getNickname(),
                    appVersion = appVersionName()
                )
            }
            val tasks = tasksJob.await()
            val inventory = inventoryJob.await()
            val chat = chatJob.await()
            val bootstrapUploaded = bootstrapPending && tasks.success && inventory.success && chat.success
            if (bootstrapUploaded) {
                configStore.markBootstrapComplete(config.baseUrl)
            }
            ApiHindraxRemoteSyncResult(
                enabled = true,
                bootstrapUploaded = bootstrapUploaded,
                pushedTasks = tasks.pushed,
                pulledTasks = tasks.pulled,
                pushedInventory = inventory.pushed,
                pulledInventory = inventory.pulled,
                pushedChat = chat.pushed,
                pulledChat = chat.pulled,
                heartbeatSent = heartbeatJob.await()
            )
        }
    }

    suspend fun pushTask(task: TaskEntity) {
        if (!configStore.load().isReady) return
        runCatching {
            client.syncTasks(JSONArray().put(task.toApiJson()))
        }.onFailure { error ->
            Log.w(TAG, "Task push failed; it will be retried by autosync", error)
        }
    }

    suspend fun pushInventory(item: InventoryEntity) {
        if (!configStore.load().isReady) return
        runCatching {
            client.syncInventory(JSONArray().put(item.toApiJson()))
        }.onFailure { error ->
            Log.w(TAG, "Inventory push failed; it will be retried by autosync", error)
        }
    }

    suspend fun pushChatMessage(message: ChatMessageEntity) {
        if (!configStore.load().isReady) return
        runCatching {
            client.syncChat(JSONArray().put(message.toApiJson()))
        }.onFailure { error ->
            Log.w(TAG, "Chat push failed; it will be retried by autosync", error)
        }
    }

    private suspend fun syncTasks(): RemoteCollectionSyncResult {
        val local = taskDao.getAllTasksForRemoteSync()
        client.syncTasks(JSONArray().also { array ->
            local.forEach { array.put(it.toApiJson()) }
        }) ?: return RemoteCollectionSyncResult(success = false, pushed = local.size)
        val remoteItems = client.listTasks()?.items ?: JSONArray()
        val applied = applyRemoteTasks(remoteItems)
        return RemoteCollectionSyncResult(success = true, pushed = local.size, pulled = applied)
    }

    private suspend fun syncInventory(): RemoteCollectionSyncResult {
        compactLocalInventoryDuplicates()
        val local = inventoryDao.getAllInventorySync()
        client.syncInventory(JSONArray().also { array ->
            local.forEach { array.put(it.toApiJson()) }
        }) ?: return RemoteCollectionSyncResult(success = false, pushed = local.size)
        val remoteItems = client.listInventory()?.items ?: JSONArray()
        val applied = applyRemoteInventory(remoteItems)
        return RemoteCollectionSyncResult(success = true, pushed = local.size, pulled = applied)
    }

    private suspend fun compactLocalInventoryDuplicates() {
        val duplicateIds = inventoryDao.getAllInventorySync()
            .groupBy { ProductNameNormalizer.key(it.name) }
            .values
            .flatMap { group ->
                if (group.size <= 1) {
                    emptyList()
                } else {
                    val keep = group.maxWith(compareBy<InventoryEntity> { it.updatedAt }.thenBy { it.id })
                    group.filter { it.id != keep.id }.map { it.id }
                }
            }
        if (duplicateIds.isNotEmpty()) {
            inventoryDao.deleteByIds(duplicateIds)
        }
    }

    private suspend fun repairInventoryFromCompletedShoppingTasks() {
        val tasks = taskDao.getAllTasksSync()
        tasks
            .filter { it.status == TaskStatus.COMPLETADA }
            .filter { it.type == TaskType.SHOPPING || it.type == TaskType.FERIA }
            .forEach { task ->
                val category = if (task.type == TaskType.FERIA) "FERIA" else "COMPRAS"
                task.checklist
                    .filter { it.text.isNotBlank() }
                    .forEach { checklistItem ->
                        ensureInventoryProduct(checklistItem.text, checklistItem.unit, category)
                    }

                task.checklist
                    .filter { it.isChecked && it.quantity != null }
                    .forEach { checklistItem ->
                        val appliedKey = inventoryLineAppliedKey(task, checklistItem)
                        if (taskDao.countHistoryByActionAndDetail(task.id, "INVENTORY_LINE_APPLIED", appliedKey) > 0) {
                            return@forEach
                        }
                        val item = inventoryDao.getByNameNormalized(ProductNameNormalizer.displayName(checklistItem.text))
                            ?: return@forEach
                        val amount = checklistItem.quantity ?: return@forEach
                        inventoryDao.updateQuantity(
                            id = item.id,
                            newQuantity = item.currentQuantity + amount,
                            updatedAt = System.currentTimeMillis()
                        )
                        taskDao.insertHistory(
                            TaskHistoryEntity(
                                taskId = task.id,
                                action = "INVENTORY_LINE_APPLIED",
                                detail = appliedKey,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
            }
    }

    private suspend fun ensureInventoryProduct(rawName: String, unit: String?, category: String): InventoryEntity? {
        val itemName = ProductNameNormalizer.displayName(rawName)
        if (itemName.isBlank()) return null
        inventoryDao.getByNameNormalized(itemName)?.let { return it }
        val id = inventoryDao.insert(
            InventoryEntity(
                name = itemName,
                category = category,
                currentQuantity = 0.0,
                minQuantity = 1.0,
                unit = unit?.trim()?.takeIf { it.isNotBlank() } ?: "unid",
                updatedAt = System.currentTimeMillis()
            )
        )
        return inventoryDao.getById(id)
    }

    private fun inventoryLineAppliedKey(task: TaskEntity, checklistItem: ChecklistItem): String {
        return InventoryApplicationKey.checklistLine(task.title, task.type, checklistItem)
    }

    private suspend fun syncChat(): RemoteCollectionSyncResult {
        val local = chatDao.getAllMessagesSync()
        client.syncChat(JSONArray().also { array ->
            local.forEach { array.put(it.toApiJson()) }
        }) ?: return RemoteCollectionSyncResult(success = false, pushed = local.size)
        val remoteItems = client.listChat()?.items ?: JSONArray()
        val applied = applyRemoteChat(remoteItems)
        return RemoteCollectionSyncResult(success = true, pushed = local.size, pulled = applied)
    }

    private suspend fun applyRemoteTasks(items: JSONArray): Int {
        var applied = 0
        for (index in 0 until items.length()) {
            val json = items.optJSONObject(index) ?: continue
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: continue
            val incomingUpdatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            val existing = taskDao.getByTitleIncludingDeleted(title) ?: taskDao.getByTitleIncludingDeleted("[REMOTE] $title")
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
            val name = json.optString("name")
                .takeIf { it.isNotBlank() }
                ?.let(ProductNameNormalizer::displayName)
                ?: continue
            val incomingUpdatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            val existing = inventoryDao.getByNameNormalized(name)
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

    private suspend fun applyRemoteChat(items: JSONArray): Int {
        var applied = 0
        for (index in 0 until items.length()) {
            val json = items.optJSONObject(index) ?: continue
            val remoteDeviceId = json.optString("deviceId").takeIf { it.isNotBlank() } ?: continue
            if (remoteDeviceId == myDeviceId) continue
            val text = json.optString("message")
            val timestamp = json.optLong("timestamp", json.optLong("updatedAt", System.currentTimeMillis()))
            val peerId = remoteDeviceId
            if (chatDao.findMessage(peerId, text, timestamp, false) != null) continue

            ensureRemotePeer(peerId, json.optString("nickname", "").takeIf { it.isNotBlank() })
            chatDao.insertMessage(
                ChatMessageEntity(
                    peerId = peerId,
                    message = text,
                    timestamp = timestamp,
                    isFromMe = false,
                    status = MessageStatus.DELIVERED
                )
            )
            applied++
        }
        return applied
    }

    private suspend fun ensureRemotePeer(peerId: String, nickname: String?) {
        val existing = chatDao.getPeerById(peerId)
        chatDao.insertPeer(
            PeerEntity(
                id = peerId,
                name = existing?.name ?: "Node_${peerId.takeLast(4)}",
                nickname = existing?.nickname ?: nickname,
                lastKnownIp = existing?.lastKnownIp ?: "API_HINDRAX",
                lastSeen = System.currentTimeMillis(),
                isOnline = existing?.isOnline ?: false,
                latitude = existing?.latitude,
                longitude = existing?.longitude,
                locationAccuracy = existing?.locationAccuracy,
                locationUpdatedAt = existing?.locationUpdatedAt
            )
        )
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

    private fun ChatMessageEntity.toApiJson(): JSONObject {
        return JSONObject().apply {
            put("id", "$myDeviceId-chat-$id-$timestamp")
            put("deviceId", myDeviceId)
            put("peerId", peerId)
            put("message", message)
            put("isFromMe", isFromMe)
            put("status", status.name)
            put("timestamp", timestamp)
            put("updatedAt", timestamp)
        }
    }

    private fun appVersionName(): String? {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName
        }.getOrNull()
    }

    private companion object {
        const val TAG = "ApiHindraxRemoteSync"
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
