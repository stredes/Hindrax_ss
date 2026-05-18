package com.hindrax.ss.domain.tasks.repository

import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskHistory
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTaskById(id: Long): Flow<Task?>
    fun observeHistory(taskId: Long): Flow<List<TaskHistory>>
    suspend fun getAllTasksSync(): List<Task>

    suspend fun createTask(
        title: String,
        description: String,
        status: TaskStatus,
        type: TaskType = TaskType.GENERAL,
        scheduledTime: Long? = null,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        quantity: Double? = null,
        unit: String? = null,
        inventoryItemId: Long? = null,
        checklist: List<ChecklistItem> = emptyList()
    )

    suspend fun updateTask(task: Task)
    suspend fun updateStatus(id: Long, status: TaskStatus)
    suspend fun deleteTask(id: Long)
}
