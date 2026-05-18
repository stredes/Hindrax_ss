package com.hindrax.ss.data.cyd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hindrax.ss.core.util.NetworkUtils
import com.hindrax.ss.domain.cyd.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CydRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) : CydRepository {

    private var connectedDevice: CydDevice? = null
    private val logFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    private var webSocket: WebSocket? = null
    private var serialPort: UsbSerialPort? = null
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bleScanner = bluetoothManager.adapter?.bluetoothLeScanner

    @SuppressLint("MissingPermission")
    override fun discoverDevices(): Flow<List<CydDevice>> = flow {
        val devices = mutableListOf<CydDevice>()

        // 1. USB Discovery
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).forEach { driver ->
            devices.add(CydDevice(
                id = "usb_${driver.device.deviceId}",
                name = "Bruce (USB Serial)",
                connectionType = ConnectionType.USB_SERIAL,
                capabilities = listOf("SERIAL_SHELL", "FAST_LOGS")
            ))
        }

        // 2. Wi-Fi Discovery (API Scan)
        NetworkUtils.getLocalIpAddress()?.let { localIp ->
            val subnet = localIp.substringBeforeLast(".")
            coroutineScope {
                val wifiJobs = (1..254).map { i ->
                    async { checkIpForBruce("$subnet.$i") }
                }
                devices.addAll(wifiJobs.awaitAll().filterNotNull())
            }
        }

        // 3. BLE Discovery (Simplified Scan)
        if (bleScanner != null) {
            // Emite lo encontrado hasta ahora mientras el scanner corre en background
            emit(devices.toList())
        } else {
            emit(devices.toList())
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun checkIpForBruce(ip: String): CydDevice? {
        val request = Request.Builder().url("http://$ip/api/info").build()
        return try {
            val fastClient = httpClient.newBuilder()
                .connectTimeout(250, TimeUnit.MILLISECONDS)
                .build()
            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    CydDevice(
                        id = "wifi_$ip",
                        name = json.optString("device", "Bruce CYD"),
                        ipAddress = ip,
                        firmwareVersion = json.optString("version"),
                        connectionType = ConnectionType.WIFI_HTTP,
                        capabilities = json.optJSONArray("modules")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()
                    )
                } else null
            }
        } catch (e: Exception) { null }
    }

    override suspend fun connect(device: CydDevice): Result<Unit> {
        connectedDevice = device
        return try {
            when (device.connectionType) {
                ConnectionType.WIFI_HTTP -> setupWebSocket(device)
                ConnectionType.USB_SERIAL -> connectUsb(device)
                else -> {}
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun connectUsb(device: CydDevice) {
        val deviceId = device.id.removePrefix("usb_").toInt()
        val usbDevice = usbManager.deviceList.values.find { it.deviceId == deviceId } ?: return
        val driver = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
        val connection = usbManager.openDevice(driver.device)
        serialPort = driver.ports[0].apply {
            open(connection)
            setParameters(115200, 8, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1)
        }
        
        GlobalScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            while (connectedDevice?.connectionType == ConnectionType.USB_SERIAL) {
                val len = serialPort?.read(buffer, 1000) ?: 0
                if (len > 0) logFlow.emit(String(buffer, 0, len))
            }
        }
    }

    private fun setupWebSocket(device: CydDevice) {
        val ip = device.ipAddress ?: return
        val request = Request.Builder().url("ws://$ip/ws/logs").build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                logFlow.tryEmit(text)
            }
        })
    }

    override suspend fun disconnect() {
        webSocket?.close(1000, "Disconnected")
        serialPort?.close()
        connectedDevice = null
    }

    override fun observeStatus(): Flow<DeviceStatus> = flow {
        while (connectedDevice != null) {
            getDeviceInfo().onSuccess { emit(DeviceStatus(activeModules = it.capabilities)) }
            delay(5000)
        }
    }

    override fun observeLogs(): Flow<String> = logFlow

    override suspend fun getDeviceInfo(): Result<CydDevice> {
        val device = connectedDevice ?: return Result.failure(Exception("No connected device"))
        return if (device.connectionType == ConnectionType.WIFI_HTTP) {
            checkIpForBruce(device.ipAddress!!)?.let { Result.success(it) } ?: Result.failure(Exception("Offline"))
        } else Result.success(device)
    }

    override suspend fun executeAction(action: CydAction): Result<Unit> {
        val device = connectedDevice ?: return Result.failure(Exception("Not connected"))
        
        if (device.connectionType == ConnectionType.USB_SERIAL) {
            val cmd = when(action) {
                is CydAction.RunModule -> "${action.module} ${action.action}\n"
                CydAction.Reboot -> "reboot\n"
                else -> ""
            }
            serialPort?.write(cmd.toByteArray(), 1000)
            return Result.success(Unit)
        }

        val ip = device.ipAddress ?: return Result.failure(Exception("No IP"))
        val json = JSONObject().apply {
            when (action) {
                is CydAction.RunModule -> { put("module", action.module); put("action", action.action) }
                CydAction.Reboot -> put("action", "reboot")
                else -> {}
            }
        }
        val request = Request.Builder()
            .url("http://$ip/api/run")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            httpClient.newCall(request).execute().use { 
                if (it.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${it.code}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun uploadFile(fileName: String, content: ByteArray): Result<Unit> {
        val device = connectedDevice ?: return Result.failure(Exception("No device"))
        val ip = device.ipAddress ?: return Result.failure(Exception("No IP"))
        
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, RequestBody.create("application/octet-stream".toMediaType(), content))
            .build()
            
        val request = Request.Builder().url("http://$ip/api/upload").post(body).build()
        
        return try {
            httpClient.newCall(request).execute().use { 
                if (it.isSuccessful) Result.success(Unit) else Result.failure(IOException())
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun downloadFile(fileName: String): Result<ByteArray> {
        val device = connectedDevice ?: return Result.failure(Exception("No device"))
        val ip = device.ipAddress ?: return Result.failure(Exception("No IP"))
        val request = Request.Builder().url("http://$ip/api/download?file=$fileName").build()
        
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(response.body?.bytes() ?: byteArrayOf())
                else Result.failure(IOException())
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}
