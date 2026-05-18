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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = taskDao.observeTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeTaskById(id: Long): Flow<Task?> = taskDao.observeTaskById(id).map { it?.toDomain() }

    override fun observeHistory(taskId: Long): Flow<List<TaskHistory>> = 
        taskDao.observeHistoryByTaskId(taskId).map { entities ->
            entities.map { it.toDomain() }
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
            checklist = checklist,
            createdAt = now,
            updatedAt = now
        )
        val id = taskDao.insert(taskEntity)
        saveHistory(id, "CREACION", "Misión [${type.name}] inicializada: $title")
    }

    override suspend fun updateTask(task: Task) {
        val existing = taskDao.observeTaskById(task.id).first() ?: return
        val now = System.currentTimeMillis()
        
        taskDao.update(task.toEntity().copy(updatedAt = now))

        saveHistory(task.id, "EDICION", "Parámetros de misión actualizados.")
        
        // Sync if completed
        if (task.status == TaskStatus.COMPLETADA) {
            syncTaskWithInventory(task)
        }
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        val now = System.currentTimeMillis()
        taskDao.updateStatus(id, status, now)
        saveHistory(id, "CAMBIO_ESTADO", "Estado actualizado a: $status")

        // Sync if completed
        if (status == TaskStatus.COMPLETADA) {
            val task = taskDao.observeTaskById(id).first()?.toDomain()
            if (task != null) {
                syncTaskWithInventory(task)
            }
        }
    }

    private suspend fun syncTaskWithInventory(task: Task) {
        // 1. Sync the main task quantity (legacy or single item link)
        if (task.inventoryItemId != null && task.quantity != null) {
            val item = inventoryDao.getById(task.inventoryItemId)
            if (item != null) {
                updateItemQuantity(item, task.quantity, task.type, task.id)
            }
        }

        // 2. Sync all checked items in the checklist
        if (task.type == TaskType.SHOPPING || task.type == TaskType.FERIA) {
            task.checklist.filter { it.isChecked && it.quantity != null }.forEach { checkItem ->
                val itemName = checkItem.text.trim()
                var inventoryItem = inventoryDao.getByName(itemName)
                
                if (inventoryItem == null) {
                    // Create product if it doesn't exist
                    val newId = inventoryDao.insert(
                        InventoryEntity(
                            name = itemName,
                            category = if (task.type == TaskType.FERIA) "FERIA" else "COMPRAS",
                            currentQuantity = 0.0,
                            minQuantity = 1.0,
                            unit = checkItem.unit ?: "unid",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    inventoryItem = inventoryDao.getById(newId)
                }
                
                if (inventoryItem != null && checkItem.quantity != null) {
                    updateItemQuantity(inventoryItem, checkItem.quantity, task.type, task.id)
                }
            }
        }
    }

    private suspend fun updateItemQuantity(item: InventoryEntity, amount: Double, type: TaskType, taskId: Long) {
        val newQuantity = when (type) {
            TaskType.SHOPPING, TaskType.FERIA -> item.currentQuantity + amount
            TaskType.INVENTORY -> amount // Set direct
            else -> item.currentQuantity
        }
        
        inventoryDao.updateQuantity(item.id, newQuantity, System.currentTimeMillis())
        saveHistory(taskId, "INVENTARIO_SYNC", "Sincronizado: ${item.name} +$amount -> Total: $newQuantity")
    }

    override suspend fun deleteTask(id: Long) {
        val now = System.currentTimeMillis()
        taskDao.softDelete(id, now)
        saveHistory(id, "ELIMINACION", "Misión terminada y archivada.")
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
    inventoryItemId, checklist, createdAt, updatedAt
)

fun Task.toEntity() = TaskEntity(
    id, title, description, status, type, scheduledTime,
    locationName, latitude, longitude, quantity, unit,
    inventoryItemId, checklist, createdAt, updatedAt
)

fun TaskHistoryEntity.toDomain() = TaskHistory(id, taskId, action, detail, createdAt)
