package com.hindrax.ss.domain.cyd

data class CydDevice(
    val id: String,
    val name: String,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val firmwareVersion: String? = null,
    val connectionType: ConnectionType,
    val capabilities: List<String> = emptyList(),
    val isConnected: Boolean = false
)

enum class ConnectionType {
    WIFI_HTTP,
    WEBSOCKET,
    BLE,
    USB_SERIAL
}

data class DeviceStatus(
    val batteryPercent: Int? = null,
    val wifiSignal: Int? = null,
    val freeHeap: Long? = null,
    val uptimeSeconds: Long? = null,
    val activeModules: List<String> = emptyList()
)

sealed class CydAction {
    data class RunModule(val module: String, val action: String) : CydAction()
    object Reboot : CydAction()
    object StopAll : CydAction()
}
