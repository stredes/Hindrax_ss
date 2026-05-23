package com.hindrax.ss.core.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.hindrax.ss.domain.profile.HindraxProfileCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleIdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdManager: DeviceIdManager
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser = adapter?.bluetoothLeAdvertiser

    private val HINDRAX_SERVICE_UUID = UUID.fromString("0000dead-0000-1000-8000-00805f9b34fb")

    fun startAdvertising() {
        if (advertiser == null || !hasRequiredPermissions()) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val myHash = deviceIdManager.getDeviceId()
        val advertisedIdentity = HindraxProfileCodec.encodePairingIdentity(
            myHash,
            deviceIdManager.getNickname().take(10)
        )
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(HINDRAX_SERVICE_UUID))
            .addServiceData(ParcelUuid(HINDRAX_SERVICE_UUID), advertisedIdentity.toByteArray())
            .setIncludeDeviceName(false)
            .build()

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || 
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                adapter?.name = myHash
            }
            
            advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
                override fun onStartFailure(errorCode: Int) {}
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}
