package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val type: TaskType = TaskType.GENERAL,
    val scheduledTime: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val inventoryItemId: Long? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val action: String,
    val detail: String,
    val createdAt: Long
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val currentQuantity: Double,
    val minQuantity: Double,
    val unit: String,
    val updatedAt: Long
)
