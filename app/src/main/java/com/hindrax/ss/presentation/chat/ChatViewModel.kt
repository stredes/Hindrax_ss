package com.hindrax.ss.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val peers: List<PeerEntity> = emptyList(),
    val selectedPeer: PeerEntity? = null,
    val messages: List<ChatMessageEntity> = emptyList(),
    val currentMessage: String = "",
    val nicknameDraft: String = "",
    val locationStatus: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageJob: kotlinx.coroutines.Job? = null

    init {
        repository.observePeers()
            .onEach { peers ->
                _uiState.update { state ->
                    val refreshedSelected = state.selectedPeer?.let { selected ->
                        peers.find { it.id == selected.id } ?: selected
                    }
                    state.copy(
                        peers = peers,
                        selectedPeer = refreshedSelected,
                        nicknameDraft = refreshedSelected?.nickname ?: state.nicknameDraft
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectPeer(peer: PeerEntity?) {
        _uiState.update { it.copy(selectedPeer = peer, messages = emptyList(), nicknameDraft = peer?.nickname ?: "") }
        messageJob?.cancel()
        
        if (peer != null) {
            messageJob = repository.observeMessages(peer.id)
                .onEach { messages -> _uiState.update { it.copy(messages = messages) } }
                .launchIn(viewModelScope)
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(currentMessage = text) }
    }

    fun onNicknameChange(text: String) {
        _uiState.update { it.copy(nicknameDraft = text) }
    }

    fun saveNickname() {
        val peerId = _uiState.value.selectedPeer?.id ?: return
        val nickname = _uiState.value.nicknameDraft
        viewModelScope.launch {
            repository.updatePeerNickname(peerId, nickname)
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val peerId = state.selectedPeer?.id ?: return
        val text = state.currentMessage
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(peerId, text)
            _uiState.update { it.copy(currentMessage = "") }
        }
    }

    fun syncFamilyData() {
        val peerId = _uiState.value.selectedPeer?.id ?: return
        viewModelScope.launch {
            repository.syncAllWithPeer(peerId)
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch {
            repository.syncAllDevices()
        }
    }

    fun shareLocationWithSelected() {
        val peerId = _uiState.value.selectedPeer?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(locationStatus = "GPS_SHARE_PENDING") }
            val result = repository.shareMyLocation(peerId)
            _uiState.update {
                it.copy(locationStatus = if (result.isSuccess) "GPS_SHARED" else "GPS_ERROR: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun shareLocationWithAllDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(locationStatus = "GPS_BROADCAST_PENDING") }
            val result = repository.shareMyLocationWithAllPeers()
            _uiState.update {
                it.copy(locationStatus = if (result.isSuccess) "GPS_BROADCAST_SHARED" else "GPS_ERROR: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
