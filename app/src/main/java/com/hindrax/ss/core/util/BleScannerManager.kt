package com.hindrax.ss.core.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
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
            close()
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Intentar obtener el hash de 3 fuentes:
                // 1. Datos del servicio (lo más fiable)
                val serviceData = result.scanRecord?.serviceData?.get(ParcelUuid(HINDRAX_SERVICE_UUID))
                val hashFromData = serviceData?.let { String(it) }

                // 2. Nombre del dispositivo
                val deviceName = result.device.name ?: result.scanRecord?.deviceName
                
                val finalHash = when {
                    hashFromData != null && hashFromData.startsWith("HNDX-") -> hashFromData
                    deviceName != null && deviceName.startsWith("HNDX-") -> deviceName
                    else -> null
                }

                if (finalHash != null) {
                    trySend(BleDiscoveredNode(finalHash, result.device.address))
                }
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
}

data class BleDiscoveredNode(
    val hash: String,
    val macAddress: String
)
