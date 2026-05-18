package com.hindrax.ss.presentation.cyd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.domain.cyd.CydRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileTransferUiState(
    val files: List<String> = emptyList(),
    val isUploading: Boolean = false,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CydFileTransferViewModel @Inject constructor(
    private val repository: CydRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileTransferUiState())
    val uiState: StateFlow<FileTransferUiState> = _uiState.asStateFlow()

    init {
        refreshFileList()
    }

    fun refreshFileList() {
        viewModelScope.launch {
            // Bruce firmware typically list files via /api/files or similar
            // This is a placeholder as the repository needs extension for file listing
            _uiState.update { it.copy(files = listOf("capture_sub.sub", "nfc_dump.bin", "bruce_logs.txt")) }
        }
    }

    fun uploadFile(fileName: String, content: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null) }
            repository.uploadFile(fileName, content)
                .onSuccess {
                    refreshFileList()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun downloadFile(fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, errorMessage = null) }
            repository.downloadFile(fileName)
                .onSuccess {
                    // Handle downloaded bytes (e.g. save to local storage)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            _uiState.update { it.copy(isDownloading = false) }
        }
    }
}
