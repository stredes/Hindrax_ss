package com.hindrax.ss.presentation.cyd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.cyd.CydAction
import com.hindrax.ss.domain.cyd.CydRepository
import com.hindrax.ss.domain.cyd.ExecuteRemoteActionUseCase
import com.hindrax.ss.domain.cyd.ObserveCydLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val consoleOutput: List<String> = emptyList(),
    val isConnected: Boolean = false
)

@HiltViewModel
class CydTerminalViewModel @Inject constructor(
    private val observeCydLogsUseCase: ObserveCydLogsUseCase,
    private val executeRemoteActionUseCase: ExecuteRemoteActionUseCase,
    private val repository: CydRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        observeCydLogsUseCase()
            .onEach { line ->
                _uiState.update { it.copy(consoleOutput = (it.consoleOutput + line).takeLast(200)) }
            }
            .launchIn(viewModelScope)
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(consoleOutput = it.consoleOutput + "> $command") }
            // Bruce firmware via Web o Serial acepta comandos directos
            // Aquí lo mapeamos como una acción genérica de ejecución
            repository.executeAction(CydAction.RunModule("shell", command))
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(consoleOutput = emptyList()) }
    }
}
