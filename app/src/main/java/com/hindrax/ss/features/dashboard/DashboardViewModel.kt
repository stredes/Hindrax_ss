package com.hindrax.ss.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.util.UpdateInfo
import com.hindrax.ss.core.util.UpdateManager
import com.hindrax.ss.core.util.UpdateResult
import com.hindrax.ss.data.remote.ApiHindraxConfigStore
import com.hindrax.ss.domain.sync.ApiHindraxStatusLabel
import com.hindrax.ss.termux.TermuxBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

data class DashboardUiState(
    val localIp: String = "Unknown",
    val isTermuxInstalled: Boolean = false,
    val recentSessionsCount: Int = 0,
    val updateAvailable: Boolean = false,
    val newVersion: String? = null,
    val updateInfo: UpdateInfo? = null,
    val updateStatus: String = "UPDATE_IDLE",
    val isCheckingUpdates: Boolean = false,
    val apiHindraxStatus: String = "DISABLED",
    val isApiHindraxOnline: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val updateManager: UpdateManager,
    private val apiHindraxConfigStore: ApiHindraxConfigStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refreshStatus(context: Context, currentVersion: String) {
        viewModelScope.launch {
            val ip = getLocalIpAddress() ?: "Disconnected"
            val termux = TermuxBridge.isTermuxInstalled(context)
            val apiConfig = apiHindraxConfigStore.load()
            val apiStatus = ApiHindraxStatusLabel.status(
                enabled = apiConfig.enabled,
                baseUrl = apiConfig.baseUrl,
                token = apiConfig.token
            )
            
            _uiState.value = _uiState.value.copy(
                localIp = ip,
                isTermuxInstalled = termux,
                apiHindraxStatus = apiStatus,
                isApiHindraxOnline = apiStatus == "ONLINE"
            )

            updateManager.getCachedUpdate(currentVersion)?.let { cached ->
                _uiState.value = _uiState.value.copy(
                    updateAvailable = true,
                    newVersion = cached.info.version,
                    updateInfo = cached.info,
                    updateStatus = "UPDATE_READY_FROM_CACHE"
                )
            }
            
            checkUpdates(currentVersion)
        }
    }

    private fun checkUpdates(currentVersion: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingUpdates = true, updateStatus = "CHECKING_GITHUB_RELEASES")
            when (val result = updateManager.checkForUpdates(currentVersion)) {
                is UpdateResult.Available -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdates = false,
                        updateAvailable = true,
                        newVersion = result.info.version,
                        updateInfo = result.info,
                        updateStatus = "UPDATE_READY"
                    )
                }
                is UpdateResult.NoUpdate -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdates = false,
                        updateAvailable = false,
                        newVersion = null,
                        updateInfo = null,
                        updateStatus = result.reason
                    )
                }
                is UpdateResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdates = false,
                        updateStatus = "UPDATE_CHECK_ERROR: ${result.message}"
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val info = _uiState.value.updateInfo ?: return
        _uiState.value = _uiState.value.copy(updateStatus = "DOWNLOADING_UPDATE")
        updateManager.downloadAndInstall(info) { status ->
            _uiState.value = _uiState.value.copy(updateStatus = status)
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
