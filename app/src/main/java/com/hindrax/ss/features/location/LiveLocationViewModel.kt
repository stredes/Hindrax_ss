package com.hindrax.ss.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LiveLocationUiState(
    val isTracking: Boolean = false,
    val permissionGranted: Boolean = false,
    val peers: List<PeerEntity> = emptyList(),
    val provider: String = "OFFLINE",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val updatedAt: Long? = null,
    val status: String = "GPS_STANDBY",
    val peerLocationStatus: String? = null,
    val error: String? = null
) {
    val hasFix: Boolean
        get() = latitude != null && longitude != null
}

@HiltViewModel
class LiveLocationViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveLocationUiState())
    val uiState = _uiState.asStateFlow()

    private var locationManager: LocationManager? = null
    private var listener: LocationListener? = null

    init {
        chatRepository.observePeers()
            .onEach { peers -> _uiState.update { it.copy(peers = peers) } }
            .launchIn(viewModelScope)
    }

    fun refreshPermission(context: Context) {
        _uiState.value = _uiState.value.copy(permissionGranted = hasLocationPermission(context))
    }

    fun startTracking(context: Context) {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) {
            _uiState.value = _uiState.value.copy(
                permissionGranted = false,
                isTracking = false,
                status = "GPS_PERMISSION_REQUIRED"
            )
            return
        }

        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = selectProvider(manager)
        if (provider == null) {
            _uiState.value = _uiState.value.copy(
                permissionGranted = true,
                isTracking = false,
                status = "GPS_PROVIDER_DISABLED",
                error = "Enable GPS or network location provider"
            )
            return
        }

        stopTracking()
        locationManager = manager
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _uiState.value = _uiState.value.copy(
                    isTracking = true,
                    permissionGranted = true,
                    provider = location.provider ?: provider,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
                    altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
                    speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
                    bearingDegrees = location.bearing.takeIf { location.hasBearing() },
                    updatedAt = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    status = "GPS_LIVE",
                    error = null
                )
            }

            override fun onProviderEnabled(provider: String) {
                _uiState.value = _uiState.value.copy(status = "PROVIDER_ENABLED: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                _uiState.value = _uiState.value.copy(status = "PROVIDER_DISABLED: $provider")
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        try {
            manager.getLastKnownLocation(provider)?.let { listener?.onLocationChanged(it) }
            manager.requestLocationUpdates(
                provider,
                MIN_UPDATE_MS,
                MIN_UPDATE_METERS,
                listener as LocationListener,
                Looper.getMainLooper()
            )
            _uiState.value = _uiState.value.copy(
                isTracking = true,
                permissionGranted = true,
                provider = provider,
                status = "GPS_WAITING_FOR_FIX",
                error = null
            )
        } catch (security: SecurityException) {
            _uiState.value = _uiState.value.copy(
                isTracking = false,
                permissionGranted = false,
                status = "GPS_PERMISSION_DENIED",
                error = security.message
            )
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(
                isTracking = false,
                status = "GPS_ERROR",
                error = error.message
            )
        }
    }

    fun stopTracking() {
        val currentListener = listener
        if (currentListener != null) {
            runCatching { locationManager?.removeUpdates(currentListener) }
        }
        listener = null
        locationManager = null
        _uiState.value = _uiState.value.copy(
            isTracking = false,
            status = if (_uiState.value.hasFix) "GPS_PAUSED" else "GPS_STANDBY"
        )
    }

    fun shareMyLocationWithAllDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(peerLocationStatus = "BROADCASTING_MY_LOCATION") }
            val result = chatRepository.shareMyLocationWithAllPeers()
            _uiState.update {
                it.copy(
                    peerLocationStatus = if (result.isSuccess) {
                        "MY_LOCATION_BROADCAST_SENT"
                    } else {
                        "BROADCAST_ERROR: ${result.exceptionOrNull()?.message}"
                    }
                )
            }
        }
    }

    fun requestAllPeerLocations() {
        viewModelScope.launch {
            _uiState.update { it.copy(peerLocationStatus = "REQUESTING_PEER_LOCATIONS") }
            val result = chatRepository.requestAllPeerLocations()
            _uiState.update {
                it.copy(
                    peerLocationStatus = if (result.isSuccess) {
                        "PEER_LOCATION_REQUESTS_SENT"
                    } else {
                        "REQUEST_ERROR: ${result.exceptionOrNull()?.message}"
                    }
                )
            }
        }
    }

    fun requestPeerLocation(peerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(peerLocationStatus = "REQUESTING_LOCATION: $peerId") }
            val result = chatRepository.requestPeerLocation(peerId)
            _uiState.update {
                it.copy(
                    peerLocationStatus = if (result.isSuccess) {
                        "LOCATION_REQUEST_SENT: $peerId"
                    } else {
                        "REQUEST_ERROR: ${result.exceptionOrNull()?.message}"
                    }
                )
            }
        }
    }

    override fun onCleared() {
        stopTracking()
        super.onCleared()
    }

    companion object {
        private const val MIN_UPDATE_MS = 1000L
        private const val MIN_UPDATE_METERS = 0f

        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        }

        fun formatTimestamp(value: Long?): String {
            if (value == null) return "--"
            return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(value))
        }

        private fun selectProvider(manager: LocationManager): String? {
            return when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
        }
    }
}
