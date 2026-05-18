package com.hindrax.ss.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.util.UpdateManager
import com.hindrax.ss.core.util.UpdateResult
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
    val updateUrl: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val updateManager: UpdateManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refreshStatus(context: Context, currentVersion: String) {
        viewModelScope.launch {
            val ip = getLocalIpAddress() ?: "Disconnected"
            val termux = TermuxBridge.isTermuxInstalled(context)
            
            _uiState.value = _uiState.value.copy(
                localIp = ip,
                isTermuxInstalled = termux
            )
            
            checkUpdates(currentVersion)
        }
    }

    private fun checkUpdates(currentVersion: String) {
        viewModelScope.launch {
            when (val result = updateManager.checkForUpdates(currentVersion)) {
                is UpdateResult.Available -> {
                    _uiState.value = _uiState.value.copy(
                        updateAvailable = true,
                        newVersion = result.version,
                        updateUrl = result.url
                    )
                }
                else -> {}
            }
        }
    }

    fun installUpdate() {
        val url = _uiState.value.updateUrl ?: return
        updateManager.downloadAndInstall(url)
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
