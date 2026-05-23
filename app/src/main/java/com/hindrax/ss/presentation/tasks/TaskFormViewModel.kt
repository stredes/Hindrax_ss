package com.hindrax.ss.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.InventoryItem
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import com.hindrax.ss.domain.tasks.usecase.CreateTaskUseCase
import com.hindrax.ss.domain.tasks.usecase.ObserveTaskDetailUseCase
import com.hindrax.ss.domain.tasks.usecase.UpdateTaskUseCase
import com.hindrax.ss.presentation.inventory.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskFormUiState(
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDIENTE,
    val type: TaskType = TaskType.GENERAL,
    val scheduledTime: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val inventoryItemId: Long? = null,
    val assignedPeerId: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val availableInventory: List<InventoryItem> = emptyList(),
    val availablePeers: List<PeerEntity> = emptyList()
)

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val observeTaskDetailUseCase: ObserveTaskDetailUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val inventoryDao: InventoryDao,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskFormUiState())
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    private var currentTaskId: Long = -1L

    init {
        inventoryDao.observeInventory()
            .onEach { entities ->
                _uiState.update { it.copy(availableInventory = entities.map { e -> e.toDomain() }) }
            }
            .launchIn(viewModelScope)

        chatRepository.observePeers()
            .onEach { peers ->
                _uiState.update { it.copy(availablePeers = peers) }
            }
            .launchIn(viewModelScope)
    }

    fun loadTask(taskId: Long) {
        if (taskId == -1L) return
        currentTaskId = taskId
        observeTaskDetailUseCase(taskId)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { task ->
                task?.let { t ->
                    _uiState.update { it.copy(
                        title = t.title,
                        description = t.description,
                        status = t.status,
                        type = t.type,
                        scheduledTime = t.scheduledTime,
                        locationName = t.locationName,
                        latitude = t.latitude,
                        longitude = t.longitude,
                        quantity = t.quantity,
                        unit = t.unit,
                        inventoryItemId = t.inventoryItemId,
                        assignedPeerId = t.assignedPeerId,
                        checklist = t.checklist,
                        createdAt = t.createdAt,
                        updatedAt = t.updatedAt,
                        isLoading = false
                    ) }
                }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onStatusChange(newStatus: TaskStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    fun onTypeChange(newType: TaskType) {
        _uiState.update { it.copy(type = newType) }
    }

    fun onScheduledTimeChange(timestamp: Long?) {
        _uiState.update { it.copy(scheduledTime = timestamp) }
    }

    fun onLocationChange(name: String, lat: Double?, lon: Double?) {
        _uiState.update { it.copy(locationName = name, latitude = lat, longitude = lon) }
    }

    fun onQuantityChange(quantity: Double?, unit: String?) {
        _uiState.update { it.copy(quantity = quantity, unit = unit) }
    }
    
    fun onInventoryLinkChange(itemId: Long?) {
        val selectedItem = _uiState.value.availableInventory.find { it.id == itemId }
        _uiState.update { it.copy(
            inventoryItemId = itemId,
            unit = selectedItem?.unit ?: it.unit,
            title = if (it.title.isBlank()) "Suministro: ${selectedItem?.name ?: ""}" else it.title
        ) }
    }

    fun onAssignedPeerChange(peerId: String?) {
        _uiState.update { it.copy(assignedPeerId = peerId) }
    }

    fun addChecklistItem(text: String, quantity: Double? = null, unit: String? = null) {
        if (text.isBlank()) return
        val newItem = ChecklistItem(
            id = UUID.randomUUID().toString(), 
            text = text,
            quantity = quantity,
            unit = unit
        )
        _uiState.update { it.copy(checklist = it.checklist + newItem) }
    }

    fun updateChecklistItem(id: String, text: String? = null, quantity: Double? = null, unit: String? = null, isChecked: Boolean? = null) {
        _uiState.update { state ->
            state.copy(checklist = state.checklist.map {
                if (it.id == id) {
                    it.copy(
                        text = text ?: it.text,
                        quantity = quantity ?: it.quantity,
                        unit = unit ?: it.unit,
                        isChecked = isChecked ?: it.isChecked
                    )
                } else it
            })
        }
    }

    fun removeChecklistItem(id: String) {
        _uiState.update { it.copy(checklist = it.checklist.filter { item -> item.id != id }) }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(checklist = state.checklist.map {
                if (it.id == id) it.copy(isChecked = !it.isChecked) else it
            })
        }
    }

    fun saveTask() {
        if (_uiState.value.title.isBlank()) {
            _uiState.update { it.copy(error = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                if (currentTaskId == -1L) {
                    createTaskUseCase(
                        title = state.title,
                        description = state.description,
                        status = state.status,
                        type = state.type,
                        scheduledTime = state.scheduledTime,
                        locationName = state.locationName,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        quantity = state.quantity,
                        unit = state.unit,
                        inventoryItemId = state.inventoryItemId,
                        assignedPeerId = state.assignedPeerId,
                        checklist = state.checklist
                    )
                } else {
                    updateTaskUseCase(
                        Task(
                            id = currentTaskId,
                            title = state.title,
                            description = state.description,
                            status = state.status,
                            type = state.type,
                            scheduledTime = state.scheduledTime,
                            locationName = state.locationName,
                            latitude = state.latitude,
                            longitude = state.longitude,
                            quantity = state.quantity,
                            unit = state.unit,
                            inventoryItemId = state.inventoryItemId,
                            assignedPeerId = state.assignedPeerId,
                            checklist = state.checklist,
                            createdAt = state.createdAt,
                            updatedAt = state.updatedAt
                        )
                    )
                }
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
