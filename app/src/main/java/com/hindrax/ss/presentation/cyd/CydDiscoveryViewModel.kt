package com.hindrax.ss.presentation.cyd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.cyd.ConnectToCydUseCase
import com.hindrax.ss.domain.cyd.CydDevice
import com.hindrax.ss.domain.cyd.DiscoverCydDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveryUiState(
    val devices: List<CydDevice> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CydDiscoveryViewModel @Inject constructor(
    private val discoverCydDevicesUseCase: DiscoverCydDevicesUseCase,
    private val connectToCydUseCase: ConnectToCydUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    fun startDiscovery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            discoverCydDevicesUseCase()
                .onStart { /* Optionally clear list */ }
                .catch { e -> _uiState.update { it.copy(isScanning = false, errorMessage = e.message) } }
                .collect { foundDevices ->
                    _uiState.update { it.copy(devices = foundDevices, isScanning = false) }
                }
        }
    }

    fun connectToDevice(device: CydDevice, onSuccess: () -> Unit) {
        viewModelScope.launch {
            connectToCydUseCase(device)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
                }
        }
    }
}
