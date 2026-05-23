package com.hindrax.ss.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.remote.ApiHindraxRemoteSyncRepository
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.domain.tasks.model.Task
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.usecase.ObserveTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: TaskStatus? = null,
    val peerNamesById: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val chatRepository: ChatRepository,
    private val remoteSyncRepository: ApiHindraxRemoteSyncRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)

    val uiState: StateFlow<TaskListUiState> = combine(
        observeTasksUseCase(),
        chatRepository.observePeers(),
        _searchQuery,
        _selectedStatus
    ) { tasks, peers, query, status ->
        val filteredTasks = tasks.filter { task ->
            (status == null || task.status == status) &&
            (task.title.contains(query, ignoreCase = true) || 
             task.description.contains(query, ignoreCase = true))
        }
        val peerNames = peers.associate { it.id to it.displayName }
        TaskListUiState(
            tasks = filteredTasks,
            searchQuery = query,
            selectedStatus = status,
            peerNamesById = peerNames,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState(isLoading = true)
    )

    init {
        refreshRemote()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(status: TaskStatus?) {
        _selectedStatus.value = status
    }

    fun refreshRemote() {
        viewModelScope.launch {
            runCatching { remoteSyncRepository.syncAll() }
        }
    }
}
