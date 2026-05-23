package com.hindrax.ss.features.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hindrax.ss.core.util.BleScannerManager
import com.hindrax.ss.core.util.BleIdentityManager
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.core.util.HindraxNotificationCenter
import com.hindrax.ss.core.util.NetworkUtils
import com.hindrax.ss.core.util.UpdateManager
import com.hindrax.ss.core.util.UpdateResult
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.repository.ChatRepository
import com.hindrax.ss.data.tasks.repository.toEntity
import com.hindrax.ss.domain.tasks.repository.TaskRepository
import com.hindrax.ss.domain.cyd.ConnectionType
import com.hindrax.ss.domain.cyd.CydDevice
import com.hindrax.ss.domain.cyd.CydRepository
import com.hindrax.ss.domain.profile.HindraxProfileCodec
import com.hindrax.ss.domain.profile.PairingIdentity
import com.hindrax.ss.domain.sync.NodeSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DiscoveryUiState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val logs: String = "",
    val localIp: String = "Unknown",
    val myDeviceId: String = "",
    val myNickname: String = "",
    val updateAvailable: Boolean = false,
    val latestVersion: String? = null,
    val lastScanTotalHosts: Int = 0
)

data class DiscoveredDevice(
    val ip: String? = null,
    val macAddress: String? = null,
    val hostname: String = "Unknown",
    val isReachable: Boolean = false,
    val isCyd: Boolean = false,
    val isHindraxNode: Boolean = false,
    val deviceHash: String? = null,
    val nickname: String? = null,
    val cydName: String? = null,
    val discoveryMethod: String = "NETWORK",
    val isAlreadyPaired: Boolean = false,
    val syncState: NodeSyncState = NodeSyncState.IDLE,
    val lastSyncAt: Long? = null
)

@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor(
    private val cydRepository: CydRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository,
    private val deviceIdManager: DeviceIdManager,
    private val bleScannerManager: BleScannerManager,
    private val bleIdentityManager: BleIdentityManager,
    private val updateManager: UpdateManager,
    private val notificationCenter: HindraxNotificationCenter,
    private val httpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var bleScanJob: Job? = null
    private val hindraxPort = 9999

    init {
        _uiState.update { state ->
            state.copy(
                localIp = NetworkUtils.getLocalIpAddress() ?: "0.0.0.0",
                myDeviceId = deviceIdManager.getDeviceId(),
                myNickname = deviceIdManager.getNickname()
            )
        }
        observePairedDevices()
        checkAppUpdates()
    }

    private fun observePairedDevices() {
        chatRepository.observePeers()
            .onEach { peers ->
                mergePairedDevices(peers)
            }
            .launchIn(viewModelScope)
    }

    private fun mergePairedDevices(peers: List<PeerEntity>) {
        val peerIds = peers.map { it.id }.toSet()
        _uiState.update { state ->
            val refreshedDevices = state.discoveredDevices.map { device ->
                if (device.deviceHash != null && device.deviceHash in peerIds) {
                    val peer = peers.first { it.id == device.deviceHash }
                    device.copy(
                        ip = device.ip ?: peer.lastKnownIp,
                        isAlreadyPaired = true,
                        nickname = peer.nickname ?: device.nickname,
                        syncState = if (device.syncState == NodeSyncState.PAIRING) NodeSyncState.IDLE else device.syncState
                    )
                } else {
                    device
                }
            }
            val visibleIds = refreshedDevices.mapNotNull { it.deviceHash }.toSet()
            val pairedOnlyDevices = peers
                .filter { it.id !in visibleIds }
                .map { peer ->
                    DiscoveredDevice(
                        ip = peer.lastKnownIp,
                        hostname = "Paired Hindrax Node",
                        isReachable = peer.isOnline,
                        isHindraxNode = true,
                        deviceHash = peer.id,
                        nickname = peer.nickname,
                        discoveryMethod = "PAIRED",
                        isAlreadyPaired = true
                    )
                }
            state.copy(
                discoveredDevices = (refreshedDevices + pairedOnlyDevices)
                    .sortedByDescending { it.isHindraxNode || it.isCyd }
            )
        }
    }

    fun checkAppUpdates() {
        viewModelScope.launch {
            // In a real app, current version would come from BuildConfig or PackageManager
            val currentVersion = "1.0.0"
            when (val result = updateManager.checkForUpdates(currentVersion)) {
                is UpdateResult.Available -> {
                    _uiState.update { it.copy(updateAvailable = true, latestVersion = result.info.version) }
                }
                else -> {
                    _uiState.update { it.copy(updateAvailable = false) }
                }
            }
        }
    }

    fun startDiscovery() {
        val localIp = _uiState.value.localIp
        val hosts = NetworkUtils.getLocalSubnetHosts()

        scanJob?.cancel()
        bleScanJob?.cancel()
        
        runCatching { bleIdentityManager.startAdvertising() }
        
        _uiState.update {
            it.copy(
                isScanning = true, 
                progress = 0f,
                discoveredDevices = emptyList(),
                lastScanTotalHosts = hosts.size,
                logs = buildString {
                    append("[*] Iniciando escaneo híbrido...\n")
                    append("[*] Tu ID: ${it.myDeviceId}\n")
                    append("[*] Tu nickname: ${it.myNickname}\n")
                    append("[*] LAN_ADDR: $localIp\n")
                    append("[*] LAN_HOSTS: ${hosts.size}\n")
                    append("[*] BLE_SCAN: starting\n")
                    if (hosts.isEmpty()) append("[!] LAN_SCAN: no se pudo resolver la subred local.\n")
                }
            )
        }
        viewModelScope.launch {
            mergePairedDevices(chatRepository.getAllPeers())
        }

        bleScanJob = bleScannerManager.scanForHindraxNodes()
            .onEach { bleNode ->
                addOrUpdateDevice(DiscoveredDevice(
                    macAddress = bleNode.macAddress,
                    hostname = "BLE Node",
                    isReachable = true,
                    isHindraxNode = true,
                    deviceHash = bleNode.hash,
                    nickname = bleNode.nickname,
                    discoveryMethod = "BLE"
                ))
            }
            .catch { error ->
                _uiState.update { it.copy(logs = it.logs + "[!] BLE_SCAN_ERROR: ${error.message ?: "unknown"}\n") }
            }
            .launchIn(viewModelScope)

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(20)
            if (hosts.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isScanning = false, progress = 1f, logs = it.logs + "[!] LAN_SCAN_ABORTED\n") }
                }
                return@launch
            }

            val jobs = hosts.mapIndexed { index, host ->
                async {
                    semaphore.withPermit {
                        val device = checkDevice(host)
                        if (device != null) {
                            addOrUpdateDevice(device)
                        }
                        
                        _uiState.update { it.copy(progress = (index + 1).toFloat() / hosts.size.toFloat()) }
                    }
                }
            }
            
            jobs.awaitAll()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        progress = 1f,
                        logs = it.logs + "[+] LAN_SCAN_DONE: ${hosts.size} hosts revisados.\n[+] BLE_SCAN: continua en segundo plano mientras la pantalla esté abierta.\n"
                    )
                }
            }
        }
    }

    private fun addOrUpdateDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            val isPaired = device.deviceHash?.let { chatRepository.getPeerById(it) != null } ?: false
            
            _uiState.update { currentState ->
                val currentList = currentState.discoveredDevices.toMutableList()
                val index = currentList.indexOfFirst { 
                    (it.deviceHash != null && it.deviceHash == device.deviceHash) || 
                    (it.ip != null && it.ip == device.ip)
                }

                var updatedLogs = currentState.logs
                if (index == -1) {
                    val tag = if (device.isHindraxNode) " [${device.displayName}: ${device.deviceHash}]" else ""
                    updatedLogs += "[+] Nodo Detectado (${device.discoveryMethod}): ${device.ip ?: device.macAddress}$tag\n"
                    if (device.isHindraxNode || device.isCyd) {
                        notificationCenter.notifyNodeDetected(
                            displayName = device.displayName,
                            detail = "${device.discoveryMethod}: ${device.ip ?: device.macAddress ?: "unknown"}"
                        )
                    }
                }

                val finalDevice = device.copy(isAlreadyPaired = isPaired)

                if (index != -1) {
                    val existing = currentList[index]
                    currentList[index] = existing.copy(
                        ip = finalDevice.ip ?: existing.ip,
                        macAddress = finalDevice.macAddress ?: existing.macAddress,
                        deviceHash = finalDevice.deviceHash ?: existing.deviceHash,
                        nickname = finalDevice.nickname ?: existing.nickname,
                        isHindraxNode = existing.isHindraxNode || finalDevice.isHindraxNode,
                        isAlreadyPaired = isPaired,
                        syncState = if (isPaired && existing.syncState == NodeSyncState.PAIRING) NodeSyncState.IDLE else existing.syncState,
                        lastSyncAt = existing.lastSyncAt,
                        discoveryMethod = if (finalDevice.discoveryMethod != existing.discoveryMethod) "HYBRID" else existing.discoveryMethod
                    )
                } else {
                    currentList.add(finalDevice)
                }

                currentState.copy(
                    discoveredDevices = currentList.toList().sortedByDescending { it.isHindraxNode || it.isCyd },
                    logs = updatedLogs
                )
            }
        }
    }

    fun syncFamilyData(peerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(logs = it.logs + "[*] Iniciando sincronización familiar con $peerId...\n") }
            try {
                val peerName = chatRepository.getPeerById(peerId)?.displayName ?: peerId
                markSyncState(peerId, NodeSyncState.SYNCING_TASKS)
                chatRepository.syncTasksWithPeer(peerId)
                _uiState.update { it.copy(logs = it.logs + "[+] TASK_SYNC_DONE: $peerName\n") }
                markSyncState(peerId, NodeSyncState.SYNCING_INVENTORY)
                chatRepository.syncInventoryWithPeer(peerId)
                _uiState.update { it.copy(logs = it.logs + "[+] INVENTORY_SYNC_DONE: $peerName\n") }
                markSyncState(peerId, NodeSyncState.SYNCED, System.currentTimeMillis())
                notificationCenter.notifySync(peerName, "MANUAL_SYNC_COMPLETED")
                _uiState.update { it.copy(logs = it.logs + "[SUCCESS] Sincronización completa.\n") }
            } catch (e: Exception) {
                markSyncState(peerId, NodeSyncState.ERROR)
                _uiState.update { it.copy(logs = it.logs + "[!] Error: ${e.message}\n") }
            }
        }
    }

    private suspend fun checkDevice(ip: String): DiscoveredDevice? {
        return try {
            val hindraxIdentity = getHindraxIdentity(ip, 1500)
            if (hindraxIdentity != null) {
                return DiscoveredDevice(
                    ip = ip, 
                    hostname = "Hindrax Node", 
                    isReachable = true, 
                    isHindraxNode = true,
                    deviceHash = hindraxIdentity.deviceId,
                    nickname = hindraxIdentity.nickname,
                    discoveryMethod = "NETWORK"
                )
            }
            val cydInfo = checkCydFirmware(ip)
            if (cydInfo != null) {
                return DiscoveredDevice(ip = ip, hostname = "CYD Node", isReachable = true, isCyd = true, cydName = cydInfo)
            }
            val inet = InetAddress.getByName(ip)
            if (inet.isReachable(800)) {
                DiscoveredDevice(ip = ip, hostname = inet.hostName, isReachable = true)
            } else null
        } catch (e: Exception) { null }
    }

    private fun getHindraxIdentity(ip: String, timeout: Int): PairingIdentity? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, hindraxPort), timeout)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("IDENTITY")
                writer.println(HindraxProfileCodec.encodePairingIdentity(deviceIdManager.getDeviceId(), deviceIdManager.getNickname()))
                val remoteId = reader.readLine()
                val identity = remoteId?.let { HindraxProfileCodec.decodePairingIdentity(it) }
                if (identity != null && identity.deviceId.startsWith("HNDX-")) identity else null
            }
        } catch (e: Exception) { null }
    }

    private fun checkCydFirmware(ip: String): String? {
        val request = Request.Builder().url("http://$ip/api/info").build()
        return try {
            val fastClient = httpClient.newBuilder().connectTimeout(1000, TimeUnit.MILLISECONDS).build()
            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    json.optString("device", "CYD Bruce")
                } else null
            }
        } catch (e: Exception) { null }
    }

    fun connectHindraxNode(device: DiscoveredDevice, onConnected: () -> Unit) {
        viewModelScope.launch {
            var hash = device.deviceHash
            var nickname = device.nickname
            val ip = device.ip
            hash?.let { markSyncState(it, NodeSyncState.PAIRING) }
            if (hash == null && ip != null) {
                val identity = withContext(Dispatchers.IO) { getHindraxIdentity(ip, 2500) }
                hash = identity?.deviceId
                nickname = identity?.nickname
                hash?.let { markSyncState(it, NodeSyncState.PAIRING) }
            }
            if (hash != null) {
                chatRepository.addManualPeer(hash!!, ip ?: device.macAddress ?: "0.0.0.0", nickname)
                markPairedReady(hash!!, nickname)
                _uiState.update { it.copy(logs = it.logs + "[LINKED] ${nickname ?: hash} listo para tareas+inventario.\n") }
                withContext(Dispatchers.Main) { onConnected() }
            } else {
                device.deviceHash?.let { markSyncState(it, NodeSyncState.ERROR) }
            }
        }
    }

    private fun markPairedReady(peerId: String, nickname: String?) {
        _uiState.update { state ->
            state.copy(
                discoveredDevices = state.discoveredDevices.map { device ->
                    if (device.deviceHash == peerId) {
                        device.copy(
                            isAlreadyPaired = true,
                            nickname = nickname ?: device.nickname,
                            syncState = NodeSyncState.IDLE
                        )
                    } else {
                        device
                    }
                }
            )
        }
    }

    private fun markSyncState(peerId: String, syncState: NodeSyncState, lastSyncAt: Long? = null) {
        _uiState.update { state ->
            state.copy(
                discoveredDevices = state.discoveredDevices.map { device ->
                    if (device.deviceHash == peerId) {
                        device.copy(
                            syncState = syncState,
                            lastSyncAt = lastSyncAt ?: device.lastSyncAt
                        )
                    } else {
                        device
                    }
                }
            )
        }
    }

    fun connectCyd(ip: String, name: String, onConnected: () -> Unit) {
        viewModelScope.launch {
            val device = CydDevice(
                id = "wifi_$ip",
                name = name,
                ipAddress = ip,
                connectionType = ConnectionType.WIFI_HTTP,
                isConnected = true
            )
            cydRepository.connect(device).onSuccess {
                withContext(Dispatchers.Main) { onConnected() }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        bleScanJob?.cancel()
    }
}

val DiscoveredDevice.displayName: String
    get() = nickname?.takeIf { it.isNotBlank() }
        ?: cydName?.takeIf { it.isNotBlank() }
        ?: deviceHash?.takeLast(4)?.let { "Node_$it" }
        ?: ip
        ?: macAddress
        ?: "Unknown Node"
