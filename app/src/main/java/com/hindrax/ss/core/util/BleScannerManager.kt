package com.hindrax.ss.core.util

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.hindrax.ss.domain.profile.HindraxProfileCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScannerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val bleScanner = adapter?.bluetoothLeScanner

    private val HINDRAX_SERVICE_UUID = UUID.fromString("0000dead-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun scanForHindraxNodes(): Flow<BleDiscoveredNode> = callbackFlow {
        if (bleScanner == null) {
            close(IllegalStateException("BLE_SCANNER_UNAVAILABLE"))
            return@callbackFlow
        }
        if (!hasScanPermission()) {
            close(SecurityException("BLE_SCAN_PERMISSION_DENIED"))
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Intentar obtener el hash de 3 fuentes:
                // 1. Datos del servicio (lo más fiable)
                val serviceData = result.scanRecord?.serviceData?.get(ParcelUuid(HINDRAX_SERVICE_UUID))
                val identityFromData = serviceData?.let { HindraxProfileCodec.decodePairingIdentity(String(it)) }
                val hashFromData = identityFromData?.deviceId

                // 2. Nombre del dispositivo
                val deviceName = result.device.name ?: result.scanRecord?.deviceName
                
                val finalHash = when {
                    hashFromData != null && hashFromData.startsWith("HNDX-") -> hashFromData
                    deviceName != null && deviceName.startsWith("HNDX-") -> deviceName
                    else -> null
                }

                if (finalHash != null) {
                    trySend(BleDiscoveredNode(finalHash, result.device.address, identityFromData?.nickname))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE_SCAN_FAILED_$errorCode"))
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HINDRAX_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }

        awaitClose {
            try {
                bleScanner.stopScan(scanCallback)
            } catch (e: Exception) {}
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}

data class BleDiscoveredNode(
    val hash: String,
    val macAddress: String,
    val nickname: String? = null
)
