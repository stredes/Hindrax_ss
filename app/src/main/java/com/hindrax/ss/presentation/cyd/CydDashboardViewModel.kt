package com.hindrax.ss.presentation.cyd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.cyd.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CydDashboardUiState(
    val device: CydDevice? = null,
    val status: DeviceStatus = DeviceStatus(),
    val logs: List<String> = emptyList(),
    val isExecuting: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CydDashboardViewModel @Inject constructor(
    private val executeRemoteActionUseCase: ExecuteRemoteActionUseCase,
    private val observeCydLogsUseCase: ObserveCydLogsUseCase,
    private val getCydStatusUseCase: GetCydStatusUseCase,
    private val repository: CydRepository // Added for getting current device info
) : ViewModel() {

    private val _uiState = MutableStateFlow(CydDashboardUiState())
    val uiState: StateFlow<CydDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getDeviceInfo().onSuccess { device ->
                _uiState.update { it.copy(device = device) }
            }
        }

        observeCydLogsUseCase()
            .onEach { log ->
                _uiState.update { it.copy(logs = (it.logs + log).takeLast(100)) }
            }
            .launchIn(viewModelScope)

        getCydStatusUseCase()
            .onEach { status ->
                _uiState.update { it.copy(status = status) }
            }
            .launchIn(viewModelScope)
    }

    fun runModuleAction(module: String, action: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }
            executeRemoteActionUseCase(CydAction.RunModule(module, action))
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            _uiState.update { it.copy(isExecuting = false) }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            executeRemoteActionUseCase(CydAction.Reboot)
        }
    }
}
