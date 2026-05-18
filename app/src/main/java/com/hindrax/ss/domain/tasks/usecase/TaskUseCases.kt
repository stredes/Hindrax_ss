package com.hindrax.ss.domain.tasks.usecase

import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskHistory
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import com.hindrax.ss.domain.tasks.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTasksUseCase @Inject constructor(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
}

class ObserveTaskDetailUseCase @Inject constructor(private val repository: TaskRepository) {
    operator fun invoke(id: Long): Flow<Task?> = repository.observeTaskById(id)
}

class ObserveTaskHistoryUseCase @Inject constructor(private val repository: TaskRepository) {
    operator fun invoke(taskId: Long): Flow<List<TaskHistory>> = repository.observeHistory(taskId)
}

class CreateTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(
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
    ) = repository.createTask(
        title, description, status, type, scheduledTime,
        locationName, latitude, longitude, quantity, unit, inventoryItemId, checklist
    )
}

class UpdateTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.updateTask(task)
}

class UpdateTaskStatusUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(id: Long, status: TaskStatus) =
        repository.updateStatus(id, status)
}

class DeleteTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteTask(id)
}

class SearchTasksUseCase @Inject constructor() {
    operator fun invoke(tasks: List<Task>, query: String): List<Task> {
        if (query.isBlank()) return tasks
        return tasks.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true) 
        }
    }
}

class FilterTasksByStatusUseCase @Inject constructor() {
    operator fun invoke(tasks: List<Task>, status: TaskStatus?): List<Task> {
        if (status == null) return tasks
        return tasks.filter { it.status == status }
    }
}
