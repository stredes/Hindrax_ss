package com.hindrax.ss.features.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import com.hindrax.ss.core.util.DeviceIdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HindraxProfileUiState(
    val deviceId: String = "",
    val nickname: String = "",
    val saved: Boolean = false
)

class HindraxProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HindraxProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        val manager = DeviceIdManager(context.applicationContext)
        _uiState.value = HindraxProfileUiState(
            deviceId = manager.getDeviceId(),
            nickname = manager.getNickname()
        )
    }

    fun onNicknameChange(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value, saved = false)
    }

    fun save(context: Context) {
        val manager = DeviceIdManager(context.applicationContext)
        manager.setNickname(_uiState.value.nickname)
        _uiState.value = _uiState.value.copy(
            nickname = manager.getNickname(),
            saved = true
        )
    }
}
