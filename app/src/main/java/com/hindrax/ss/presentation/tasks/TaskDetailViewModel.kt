package com.hindrax.ss.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.usecase.DeleteTaskUseCase
import com.hindrax.ss.domain.tasks.usecase.ObserveTaskDetailUseCase
import com.hindrax.ss.domain.tasks.usecase.UpdateTaskStatusUseCase
import com.hindrax.ss.domain.tasks.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val availablePeers: List<PeerEntity> = emptyList()
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val observeTaskDetailUseCase: ObserveTaskDetailUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        chatRepository.observePeers()
            .onEach { peers -> _uiState.update { it.copy(availablePeers = peers.filter { p -> p.isOnline }) } }
            .launchIn(viewModelScope)
    }

    fun loadTask(taskId: Long) {
        observeTaskDetailUseCase(taskId)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { task ->
                _uiState.update { it.copy(task = task, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun updateStatus(status: TaskStatus) {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            updateTaskStatusUseCase(taskId, status)
        }
    }

    fun toggleChecklistItem(itemId: String) {
        val currentTask = _uiState.value.task ?: return
        val updatedChecklist = currentTask.checklist.map {
            if (it.id == itemId) it.copy(isChecked = !it.isChecked) else it
        }
        viewModelScope.launch {
            updateTaskUseCase(currentTask.copy(checklist = updatedChecklist))
        }
    }

    fun shareTask(peerId: String) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            // Mapping domain model to entity for the repository method
            val entity = TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                status = task.status,
                type = task.type,
                scheduledTime = task.scheduledTime,
                locationName = task.locationName,
                latitude = task.latitude,
                longitude = task.longitude,
                quantity = task.quantity,
                unit = task.unit,
                inventoryItemId = task.inventoryItemId,
                assignedPeerId = task.assignedPeerId,
                checklist = task.checklist,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt
            )
            chatRepository.shareTask(peerId, entity)
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        val taskId = _uiState.value.task?.id ?: return
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            onDeleted()
        }
    }
}
