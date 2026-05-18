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
    val currentMessage: String = ""
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
            .onEach { peers -> _uiState.update { it.copy(peers = peers) } }
            .launchIn(viewModelScope)
    }

    fun selectPeer(peer: PeerEntity?) {
        _uiState.update { it.copy(selectedPeer = peer, messages = emptyList()) }
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
}
