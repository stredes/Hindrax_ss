package com.hindrax.ss.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.tasks.model.TaskHistory
import com.hindrax.ss.domain.tasks.usecase.ObserveTaskHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TaskHistoryUiState(
    val history: List<TaskHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskHistoryViewModel @Inject constructor(
    private val observeTaskHistoryUseCase: ObserveTaskHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskHistoryUiState())
    val uiState: StateFlow<TaskHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(taskId: Long) {
        observeTaskHistoryUseCase(taskId)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { history ->
                _uiState.update { it.copy(history = history, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
