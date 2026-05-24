package com.hindrax.ss.data.tasks.repository

import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.data.entity.TaskHistoryEntity
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskHistory
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import com.hindrax.ss.domain.tasks.repository.TaskRepository
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.domain.inventory.InventoryApplicationKey
import com.hindrax.ss.domain.inventory.ProductNameNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val chatRepository: ChatRepository
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = taskDao.observeTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeTaskById(id: Long): Flow<Task?> = taskDao.observeTaskById(id).map { it?.toDomain() }

    override fun observeHistory(taskId: Long): Flow<List<TaskHistory>> = 
        taskDao.observeHistoryByTaskId(taskId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getAllTasksSync(): List<Task> {
        return taskDao.getAllTasksSync().map { it.toDomain() }
    }

    override suspend fun createTask(
        title: String,
        description: String,
        status: TaskStatus,
        type: TaskType,
        scheduledTime: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        quantity: Double?,
        unit: String?,
        inventoryItemId: Long?,
        assignedPeerId: String?,
        checklist: List<ChecklistItem>
    ) {
        val now = System.currentTimeMillis()
        val taskEntity = TaskEntity(
            title = title,
            description = description,
            status = status,
            type = type,
            scheduledTime = scheduledTime,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            quantity = quantity,
            unit = unit,
            inventoryItemId = inventoryItemId,
            assignedPeerId = assignedPeerId,
            checklist = checklist,
            createdAt = now,
            updatedAt = now
        )
        val id = taskDao.insert(taskEntity)
        saveHistory(id, "CREACION", "Misión [${type.name}] inicializada: $title")
        ensureChecklistProducts(taskEntity.copy(id = id).toDomain())

        // Broadcast new task to known peers (best-effort)
        try {
            val inserted = taskEntity.copy(id = id)
            chatRepository.broadcastTask(inserted)
        } catch (e: Exception) { }
    }

    override suspend fun updateTask(task: Task) {
        val now = System.currentTimeMillis()
        val updatedTask = task.copy(updatedAt = now)
        taskDao.update(updatedTask.toEntity())
        saveHistory(task.id, "EDICION", "Parámetros de misión actualizados.")
        ensureChecklistProducts(updatedTask)
        
        if (updatedTask.status == TaskStatus.COMPLETADA) {
            syncTaskWithInventory(updatedTask)
        }
        // Broadcast updated task (best-effort)
        try {
            chatRepository.broadcastTask(updatedTask.toEntity())
        } catch (e: Exception) { }
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        val now = System.currentTimeMillis()
        taskDao.updateStatus(id, status, now)
        saveHistory(id, "CAMBIO_ESTADO", "Estado actualizado a: $status")

        if (status == TaskStatus.COMPLETADA) {
            val task = taskDao.observeTaskById(id).first()?.toDomain()
            if (task != null) {
                syncTaskWithInventory(task)
            }
        }
        // Broadcast status change (best-effort)
        try {
            val entity = taskDao.observeTaskById(id).first()
            if (entity != null) chatRepository.broadcastTask(entity)
        } catch (e: Exception) { }
    }

    private suspend fun syncTaskWithInventory(task: Task) {
        if (task.inventoryItemId != null && task.quantity != null) {
            val item = inventoryDao.getById(task.inventoryItemId)
            if (item != null) {
                updateItemQuantity(item, task.quantity, task.type, task.id)
            }
        }

        if (task.type == TaskType.SHOPPING || task.type == TaskType.FERIA) {
            task.checklist
                .filter { it.text.isNotBlank() }
                .forEach { checkItem ->
                    ensureInventoryProduct(
                        rawName = checkItem.text,
                        unit = checkItem.unit,
                        category = if (task.type == TaskType.FERIA) "FERIA" else "COMPRAS"
                    )
                }

            task.checklist.filter { it.isChecked && it.quantity != null }.forEach { checkItem ->
                val appliedKey = inventoryLineAppliedKey(task, checkItem)
                if (taskDao.countHistoryByActionAndDetail(task.id, "INVENTORY_LINE_APPLIED", appliedKey) > 0) {
                    return@forEach
                }
                val inventoryItem = inventoryDao.getByNameNormalized(ProductNameNormalizer.displayName(checkItem.text))
                if (inventoryItem != null && checkItem.quantity != null) {
                    updateItemQuantity(inventoryItem, checkItem.quantity, task.type, task.id)
                    saveHistory(task.id, "INVENTORY_LINE_APPLIED", appliedKey)
                }
            }
        }
    }

    private suspend fun ensureChecklistProducts(task: Task) {
        if (task.type != TaskType.SHOPPING && task.type != TaskType.FERIA) return
        val category = if (task.type == TaskType.FERIA) "FERIA" else "COMPRAS"
        task.checklist
            .filter { it.text.isNotBlank() }
            .forEach { checkItem ->
                ensureInventoryProduct(
                    rawName = checkItem.text,
                    unit = checkItem.unit,
                    category = category
                )
            }
    }

    private suspend fun ensureInventoryProduct(rawName: String, unit: String?, category: String): InventoryEntity? {
        val itemName = ProductNameNormalizer.displayName(rawName)
        if (itemName.isBlank()) return null
        inventoryDao.getByNameNormalized(itemName)?.let { return it }

        val now = System.currentTimeMillis()
        val newId = inventoryDao.insert(
            InventoryEntity(
                name = itemName,
                category = category,
                currentQuantity = 0.0,
                minQuantity = 1.0,
                unit = unit?.trim()?.takeIf { it.isNotBlank() } ?: "unid",
                updatedAt = now
            )
        )
        val created = inventoryDao.getById(newId)
        if (created != null) {
            try {
                chatRepository.broadcastInventory(created)
            } catch (e: Exception) {
            }
        }
        return created
    }

    private suspend fun updateItemQuantity(item: InventoryEntity, amount: Double, type: TaskType, taskId: Long) {
        val newQuantity = when (type) {
            TaskType.SHOPPING, TaskType.FERIA -> item.currentQuantity + amount
            TaskType.INVENTORY -> amount
            else -> item.currentQuantity
        }
        
        inventoryDao.updateQuantity(item.id, newQuantity, System.currentTimeMillis())
        saveHistory(taskId, "INVENTARIO_SYNC", "Sincronizado: ${item.name} +$amount -> Total: $newQuantity")
        // Broadcast inventory update to peers (best-effort)
        try {
            val updated = inventoryDao.getById(item.id)
            if (updated != null) chatRepository.broadcastInventory(updated)
        } catch (e: Exception) { }
    }

    private fun inventoryLineAppliedKey(task: Task, checklistItem: ChecklistItem): String {
        return InventoryApplicationKey.checklistLine(task.title, task.type, checklistItem)
    }

    override suspend fun deleteTask(id: Long) {
        val now = System.currentTimeMillis()
        taskDao.softDelete(id, now)
        saveHistory(id, "ELIMINACION", "Misión terminada y archivada.")
        try {
            val deleted = taskDao.getByIdIncludingDeleted(id)
            if (deleted != null) chatRepository.broadcastTask(deleted)
        } catch (e: Exception) {
        }
    }

    private suspend fun saveHistory(taskId: Long, action: String, detail: String) {
        taskDao.insertHistory(
            TaskHistoryEntity(
                taskId = taskId,
                action = action,
                detail = detail,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}

fun TaskEntity.toDomain() = Task(
    id, title, description, status, type, scheduledTime, 
    locationName, latitude, longitude, quantity, unit, 
    inventoryItemId, assignedPeerId, checklist, createdAt, updatedAt
)

fun Task.toEntity() = TaskEntity(
    id, title, description, status, type, scheduledTime,
    locationName, latitude, longitude, quantity, unit,
    inventoryItemId, assignedPeerId, checklist, createdAt, updatedAt
)

fun TaskHistoryEntity.toDomain() = TaskHistory(id, taskId, action, detail, createdAt)
