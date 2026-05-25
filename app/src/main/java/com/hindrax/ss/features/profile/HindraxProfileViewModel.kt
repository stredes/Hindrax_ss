package com.hindrax.ss.features.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.core.util.HindraxThemeStore
import com.hindrax.ss.data.db.HindraxDatabase
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.remote.ApiHindraxClient
import com.hindrax.ss.data.remote.ApiHindraxConfigStore
import com.hindrax.ss.domain.root.RootAccessPolicy
import com.hindrax.ss.domain.theme.HindraxThemePreset
import com.hindrax.ss.domain.theme.HindraxThemePresetCodec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class RootPeerUi(
    val id: String,
    val displayName: String,
    val ip: String,
    val isOnline: Boolean,
    val lastSeenLabel: String,
    val locationLabel: String,
    val hasLocation: Boolean
)

data class HindraxProfileUiState(
    val deviceId: String = "",
    val nickname: String = "",
    val saved: Boolean = false,
    val activeTheme: HindraxThemePreset = HindraxThemePreset(),
    val savedThemes: List<HindraxThemePreset> = listOf(HindraxThemePreset()),
    val themeImportDraft: String = "",
    val themeStatus: String? = null,
    val rootKeyDraft: String = "",
    val rootUnlocked: Boolean = false,
    val rootConfirmDraft: String = "",
    val rootStatus: String? = null,
    val rootPeers: List<RootPeerUi> = emptyList(),
    val rootBusy: Boolean = false
)

class HindraxProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HindraxProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        val manager = DeviceIdManager(context.applicationContext)
        val current = _uiState.value
        _uiState.value = current.copy(
            deviceId = manager.getDeviceId(),
            nickname = manager.getNickname(),
            activeTheme = HindraxThemeStore.loadActiveTheme(context),
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeImportDraft = current.themeImportDraft,
            themeStatus = current.themeStatus
        )
        if (current.rootUnlocked) refreshRootPeers(context)
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

    fun applyTheme(context: Context, preset: HindraxThemePreset) {
        val active = HindraxThemeStore.applyTheme(context, preset)
        _uiState.value = _uiState.value.copy(
            activeTheme = active,
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeStatus = "THEME_APPLIED: ${active.name}"
        )
    }

    fun deleteTheme(context: Context, preset: HindraxThemePreset) {
        val themes = HindraxThemeStore.deleteTheme(context, preset.name)
        _uiState.value = _uiState.value.copy(
            savedThemes = themes,
            themeStatus = "THEME_DELETED: ${preset.name}"
        )
    }

    fun themeExport(preset: HindraxThemePreset): String {
        return HindraxThemePresetCodec.encode(preset)
    }

    fun onThemeImportDraftChange(value: String) {
        _uiState.value = _uiState.value.copy(themeImportDraft = value)
    }

    fun importTheme(context: Context) {
        val decoded = runCatching {
            HindraxThemePresetCodec.decode(_uiState.value.themeImportDraft)
        }.getOrNull()
        if (decoded == null) {
            _uiState.value = _uiState.value.copy(themeStatus = "THEME_IMPORT_ERROR")
            return
        }
        val active = HindraxThemeStore.applyTheme(context, decoded)
        _uiState.value = _uiState.value.copy(
            activeTheme = active,
            savedThemes = HindraxThemeStore.loadLibrary(context),
            themeImportDraft = "",
            themeStatus = "THEME_IMPORTED_TO_PROFILE"
        )
    }

    fun onRootKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(rootKeyDraft = value, rootStatus = null)
    }

    fun onRootConfirmChange(value: String) {
        _uiState.value = _uiState.value.copy(rootConfirmDraft = value)
    }

    fun unlockRoot(context: Context) {
        if (!RootAccessPolicy.isValid(_uiState.value.rootKeyDraft)) {
            _uiState.value = _uiState.value.copy(
                rootUnlocked = false,
                rootStatus = "ROOT_DENIED"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            rootUnlocked = true,
            rootStatus = "ROOT_UNLOCKED"
        )
        refreshRootPeers(context)
    }

    fun lockRoot() {
        _uiState.value = _uiState.value.copy(
            rootKeyDraft = "",
            rootUnlocked = false,
            rootConfirmDraft = "",
            rootStatus = "ROOT_LOCKED",
            rootPeers = emptyList()
        )
    }

    fun refreshRootPeers(context: Context) {
        if (!_uiState.value.rootUnlocked) return
        _uiState.value = _uiState.value.copy(rootBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                HindraxDatabase.getDatabase(context.applicationContext)
                    .chatDao()
                    .getAllPeersSync()
                    .map { it.toRootPeerUi() }
            }
            _uiState.value = _uiState.value.copy(
                rootPeers = result.getOrDefault(emptyList()),
                rootStatus = if (result.isSuccess) "ROOT_DEVICES_REFRESHED" else "ROOT_DEVICES_ERROR",
                rootBusy = false
            )
        }
    }

    fun deleteRootPeer(context: Context, peerId: String) {
        if (!_uiState.value.rootUnlocked) return
        _uiState.value = _uiState.value.copy(rootBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val chatDao = HindraxDatabase.getDatabase(context.applicationContext).chatDao()
                chatDao.deleteMessagesWithPeer(peerId)
                chatDao.deletePeerById(peerId)
            }
            val peers = runCatching {
                HindraxDatabase.getDatabase(context.applicationContext)
                    .chatDao()
                    .getAllPeersSync()
                    .map { it.toRootPeerUi() }
            }.getOrDefault(_uiState.value.rootPeers)
            _uiState.value = _uiState.value.copy(
                rootPeers = peers,
                rootStatus = if (result.isSuccess) "ROOT_DEVICE_DELETED_LOCAL" else "ROOT_DEVICE_DELETE_ERROR",
                rootBusy = false
            )
        }
    }

    fun deleteRootPeerFromApi(context: Context, peerId: String) {
        val state = _uiState.value
        if (!state.rootUnlocked || !RootAccessPolicy.isValid(state.rootKeyDraft)) return
        _uiState.value = state.copy(rootBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = runCatching {
                ApiHindraxClient(
                    httpClient = OkHttpClient(),
                    configStore = ApiHindraxConfigStore(context.applicationContext)
                ).adminDeleteDevice(state.rootKeyDraft.trim(), peerId)
            }.getOrNull()
            _uiState.value = _uiState.value.copy(
                rootStatus = when (response) {
                    true -> "ROOT_DEVICE_DELETED_FIREBASE"
                    false -> "ROOT_API_ADMIN_ENDPOINT_PENDING"
                    null -> "ROOT_API_DISABLED_OR_ERROR"
                },
                rootBusy = false
            )
        }
    }

    fun resetLocalDatabase(context: Context) {
        val state = _uiState.value
        if (!state.rootUnlocked || state.rootConfirmDraft.trim() != RootAccessPolicy.CONFIRM_LOCAL_RESET) {
            _uiState.value = state.copy(rootStatus = "ROOT_CONFIRM_REQUIRED: ${RootAccessPolicy.CONFIRM_LOCAL_RESET}")
            return
        }
        _uiState.value = state.copy(rootBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                HindraxDatabase.getDatabase(context.applicationContext).clearAllTables()
            }
            _uiState.value = _uiState.value.copy(
                rootPeers = emptyList(),
                rootConfirmDraft = "",
                rootStatus = if (result.isSuccess) "LOCAL_DATABASE_RESET_DONE" else "LOCAL_DATABASE_RESET_ERROR",
                rootBusy = false
            )
        }
    }

    fun resetFirebaseDatabase(context: Context) {
        val state = _uiState.value
        if (!state.rootUnlocked || state.rootConfirmDraft.trim() != RootAccessPolicy.CONFIRM_FIREBASE_RESET) {
            _uiState.value = state.copy(rootStatus = "ROOT_CONFIRM_REQUIRED: ${RootAccessPolicy.CONFIRM_FIREBASE_RESET}")
            return
        }
        if (!RootAccessPolicy.isValid(state.rootKeyDraft)) {
            _uiState.value = state.copy(rootStatus = "ROOT_DENIED")
            return
        }
        _uiState.value = state.copy(rootBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = runCatching {
                ApiHindraxClient(
                    httpClient = OkHttpClient(),
                    configStore = ApiHindraxConfigStore(context.applicationContext)
                ).adminResetFirebase(state.rootKeyDraft.trim(), RootAccessPolicy.CONFIRM_FIREBASE_RESET)
            }.getOrNull()
            _uiState.value = _uiState.value.copy(
                rootConfirmDraft = "",
                rootStatus = when (response) {
                    true -> "FIREBASE_RESET_REQUEST_ACCEPTED"
                    false -> "ROOT_API_ADMIN_ENDPOINT_PENDING"
                    null -> "ROOT_API_DISABLED_OR_ERROR"
                },
                rootBusy = false
            )
        }
    }

    private fun PeerEntity.toRootPeerUi(): RootPeerUi {
        return RootPeerUi(
            id = id,
            displayName = displayName,
            ip = lastKnownIp.ifBlank { "NO_IP" },
            isOnline = isOnline,
            lastSeenLabel = formatTimestamp(lastSeen),
            locationLabel = if (hasLocation) {
                val accuracy = locationAccuracy?.let { " acc=${it.toInt()}m" }.orEmpty()
                "$locationLabel$accuracy"
            } else {
                locationLabel
            },
            hasLocation = hasLocation
        )
    }

    private fun formatTimestamp(value: Long): String {
        if (value <= 0L) return "NO_LAST_SEEN"
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(value))
        }.getOrDefault(value.toString())
    }
}
