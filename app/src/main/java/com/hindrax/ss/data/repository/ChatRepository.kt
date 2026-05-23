package com.hindrax.ss.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.core.util.HindraxNotificationCenter
import com.hindrax.ss.data.db.ChatDao
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.domain.inventory.InventorySyncItem
import com.hindrax.ss.domain.inventory.InventorySyncResolver
import com.hindrax.ss.domain.profile.HindraxProfileCodec
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeviceLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val capturedAt: Long
)

@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val deviceIdManager: DeviceIdManager,
    private val notificationCenter: HindraxNotificationCenter
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val myDeviceId = deviceIdManager.getDeviceId()
    private val HINDRAX_PORT = 9999
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observePeers(): Flow<List<PeerEntity>> = chatDao.observePeers()
    fun observeMessages(peerId: String): Flow<List<ChatMessageEntity>> = chatDao.observeMessages(peerId)
    suspend fun getAllPeers(): List<PeerEntity> = chatDao.getAllPeersSync()
    suspend fun getPeerById(id: String): PeerEntity? = chatDao.getPeerById(id)
    suspend fun updatePeerNickname(peerId: String, nickname: String?) {
        val normalized = nickname?.trim()?.takeIf { it.isNotBlank() }
        chatDao.updatePeerNickname(peerId, normalized)
    }

    init {
        acquireMulticastLock()
        scope.launch { startServer() }
    }

    private fun acquireMulticastLock() {
        try {
            multicastLock = wifiManager.createMulticastLock("HindraxLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun startServer() {
        withContext(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(HINDRAX_PORT)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    handleConnection(socket)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun handleConnection(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val header = reader.readLine() // IDENTITY, MESSAGE, TASK, INVENTORY, or LOCATION
                val remoteIdentity = HindraxProfileCodec.decodePairingIdentity(reader.readLine())
                val remoteId = remoteIdentity.deviceId
                val remoteIp = socket.inetAddress.hostAddress ?: ""

                when (header) {
                    "IDENTITY" -> {
                        writer.println(HindraxProfileCodec.encodePairingIdentity(myDeviceId, deviceIdManager.getNickname()))
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                    }
                    "MESSAGE" -> {
                        val text = reader.readLine()
                        chatDao.insertMessage(ChatMessageEntity(
                            peerId = remoteId,
                            message = text,
                            timestamp = System.currentTimeMillis(),
                            isFromMe = false
                        ))
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                        notificationCenter.notifyMessage(displayName(remoteId, remoteIdentity.nickname), text)
                    }
                    "TASK" -> {
                        val taskJson = reader.readLine()
                        receiveSharedTask(taskJson, remoteId)
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                        notificationCenter.notifySync(displayName(remoteId, remoteIdentity.nickname), "MISSION_DATA_RECEIVED")
                    }
                    "INVENTORY" -> {
                        val inventoryJson = reader.readLine()
                        receiveSharedInventory(inventoryJson, remoteId)
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                        notificationCenter.notifySync(displayName(remoteId, remoteIdentity.nickname), "INVENTORY_DATA_RECEIVED")
                    }
                    "LOCATION" -> {
                        val locationJson = reader.readLine()
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                        receiveSharedLocation(locationJson, remoteId)
                        notificationCenter.notifySync(displayName(remoteId, remoteIdentity.nickname), "LOCATION_UPDATED")
                    }
                    "LOCATION_REQUEST" -> {
                        addManualPeer(remoteId, remoteIp, remoteIdentity.nickname)
                        shareMyLocation(remoteId)
                    }
                }
                socket.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun receiveSharedTask(jsonStr: String, fromPeerId: String) {
        try {
            val json = JSONObject(jsonStr)
            val title = json.getString("title")
            
            // If task exists with same title, update its status/checklist; otherwise insert as new
            val existing = taskDao.getByTitle(title) ?: taskDao.getByTitle("[SHARED] $title")

            val checklistJson = json.optJSONArray("checklist")
            val checklist = mutableListOf<ChecklistItem>()
            if (checklistJson != null) {
                for (i in 0 until checklistJson.length()) {
                    val item = checklistJson.getJSONObject(i)
                    checklist.add(ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        text = item.getString("text"),
                        isChecked = item.optBoolean("checked", false),
                        quantity = if (item.has("q")) item.getDouble("q") else null,
                        unit = item.optString("u", "").takeIf { it.isNotBlank() }
                    ))
                }
            }
            val incomingStatus = TaskStatus.valueOf(json.optString("status", TaskStatus.PENDIENTE.name))
            val assignedPeerId = json.optString("assignedPeerId", "").takeIf { it.isNotBlank() }

            val receivedTitle: String
            if (existing != null) {
                // Update existing task's status and checklist
                val updated = existing.copy(
                    status = incomingStatus,
                    assignedPeerId = assignedPeerId,
                    checklist = checklist.ifEmpty { existing.checklist },
                    updatedAt = System.currentTimeMillis()
                )
                taskDao.update(updated)
                receivedTitle = existing.title
            } else {
                val task = TaskEntity(
                    title = title,
                    description = "Node Source: $fromPeerId\n\n${json.optString("description", "")}",
                    status = incomingStatus,
                    type = TaskType.valueOf(json.optString("type", TaskType.GENERAL.name)),
                    locationName = json.optString("location", "").takeIf { it.isNotBlank() },
                    latitude = if (json.has("lat")) json.getDouble("lat") else null,
                    longitude = if (json.has("lng")) json.getDouble("lng") else null,
                    scheduledTime = if (json.has("scheduledTime") && !json.isNull("scheduledTime")) json.getLong("scheduledTime") else null,
                    quantity = if (json.has("quantity") && !json.isNull("quantity")) json.getDouble("quantity") else null,
                    unit = json.optString("unit", "").takeIf { it.isNotBlank() },
                    inventoryItemId = if (json.has("inventoryItemId") && !json.isNull("inventoryItemId")) json.getLong("inventoryItemId") else null,
                    assignedPeerId = assignedPeerId,
                    checklist = checklist,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                taskDao.insert(task)
                receivedTitle = task.title
            }
            
            chatDao.insertMessage(ChatMessageEntity(
                peerId = fromPeerId,
                message = "📦 MISSION_RECEIVED: $receivedTitle",
                timestamp = System.currentTimeMillis(),
                isFromMe = false
            ))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun receiveSharedInventory(jsonStr: String, fromPeerId: String) {
        try {
            val json = JSONObject(jsonStr)
            val name = json.getString("name")
            val existing = inventoryDao.getByName(name)
            val incomingUpdatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            val incomingSyncItem = InventorySyncItem(
                name = name,
                currentQuantity = json.getDouble("currentQuantity"),
                updatedAt = incomingUpdatedAt
            )
            val existingSyncItem = existing?.let {
                InventorySyncItem(
                    name = it.name,
                    currentQuantity = it.currentQuantity,
                    updatedAt = it.updatedAt
                )
            }
            val syncResult = InventorySyncResolver.resolve(existingSyncItem, incomingSyncItem)
            if (!syncResult.shouldApply) return

            val item = InventoryEntity(
                id = existing?.id ?: 0,
                name = name,
                category = json.getString("category"),
                currentQuantity = json.getDouble("currentQuantity"),
                minQuantity = json.getDouble("minQuantity"),
                unit = json.getString("unit"),
                updatedAt = incomingUpdatedAt
            )
            
            inventoryDao.insert(item)
            
            chatDao.insertMessage(ChatMessageEntity(
                peerId = fromPeerId,
                message = syncResult.changeMessage,
                timestamp = System.currentTimeMillis(),
                isFromMe = false
            ))
            notificationCenter.notifySync(displayName(fromPeerId, null), syncResult.changeMessage)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun receiveSharedLocation(jsonStr: String, fromPeerId: String) {
        try {
            val json = JSONObject(jsonStr)
            val latitude = json.getDouble("latitude")
            val longitude = json.getDouble("longitude")
            val accuracy = if (json.has("accuracy") && !json.isNull("accuracy")) {
                json.getDouble("accuracy").toFloat()
            } else null
            val updatedAt = json.optLong("capturedAt", System.currentTimeMillis())

            chatDao.updatePeerLocation(fromPeerId, latitude, longitude, accuracy, updatedAt)
            chatDao.insertMessage(
                ChatMessageEntity(
                    peerId = fromPeerId,
                    message = "📍 DEVICE_LOCATION_UPDATED: ${String.format(java.util.Locale.US, "%.6f,%.6f", latitude, longitude)}",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = false
                )
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun syncAllWithPeer(peerId: String) {
        syncTasksWithPeer(peerId)
        syncInventoryWithPeer(peerId)

        chatDao.insertMessage(ChatMessageEntity(
            peerId = peerId,
            message = "🔄 FULL_FAMILY_SYNC_COMPLETED",
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        ))
        chatDao.getPeerById(peerId)?.let { peer ->
            notificationCenter.notifySync(peer.displayName, "FULL_FAMILY_SYNC_COMPLETED")
        }
    }

    suspend fun syncTasksWithPeer(peerId: String) {
        val tasks = taskDao.getAllTasksSync()
        tasks.forEach { shareTask(peerId, it) }
    }

    suspend fun syncInventoryWithPeer(peerId: String) {
        val inventory = inventoryDao.getAllInventorySync()
        inventory.forEach { shareInventoryItem(peerId, it) }
    }

    suspend fun syncAllDevices() {
        val peers = chatDao.getAllPeersSync()
        peers.forEach { peer -> syncAllWithPeer(peer.id) }
    }

    suspend fun shareMyLocation(peerId: String): Result<Unit> = runCatching {
        val peer = chatDao.getPeerById(peerId) ?: error("Peer not found")
        val location = getOwnLocation()
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            if (location.accuracy != null) put("accuracy", location.accuracy)
            put("capturedAt", location.capturedAt)
        }
        sendToPeer(peer, "LOCATION", json.toString())
        chatDao.insertMessage(
            ChatMessageEntity(
                peerId = peerId,
                message = "📍 MY_LOCATION_SHARED: ${String.format(java.util.Locale.US, "%.6f,%.6f", location.latitude, location.longitude)}",
                timestamp = System.currentTimeMillis(),
                isFromMe = true
            )
        )
    }

    suspend fun shareMyLocationWithAllPeers(): Result<Unit> = runCatching {
        val peers = chatDao.getAllPeersSync()
        peers.forEach { peer -> shareMyLocation(peer.id).getOrThrow() }
    }

    suspend fun requestPeerLocation(peerId: String): Result<Unit> = runCatching {
        val peer = chatDao.getPeerById(peerId) ?: error("Peer not found")
        sendToPeer(peer, "LOCATION_REQUEST", "{}")
        chatDao.insertMessage(
            ChatMessageEntity(
                peerId = peerId,
                message = "📡 LOCATION_REQUEST_SENT",
                timestamp = System.currentTimeMillis(),
                isFromMe = true
            )
        )
    }

    suspend fun requestAllPeerLocations(): Result<Unit> = runCatching {
        val peers = chatDao.getAllPeersSync()
        peers.forEach { peer -> requestPeerLocation(peer.id).getOrThrow() }
    }

    // Broadcast a task to all known peers
    suspend fun broadcastTask(task: TaskEntity) {
        val peers = chatDao.getAllPeersSync()
        peers.forEach { p -> shareTask(p.id, task) }
    }

    // Broadcast an inventory update to all known peers
    suspend fun broadcastInventory(item: InventoryEntity) {
        val peers = chatDao.getAllPeersSync()
        peers.forEach { p -> shareInventoryItem(p.id, item) }
    }

    suspend fun shareTask(peerId: String, task: TaskEntity) {
        val peer = chatDao.getPeerById(peerId) ?: return
        
        val taskJson = JSONObject().apply {
            put("title", task.title)
            put("description", task.description)
            put("status", task.status.name)
            put("type", task.type.name)
            if (task.locationName != null) put("location", task.locationName)
            if (task.scheduledTime != null) put("scheduledTime", task.scheduledTime)
            if (task.quantity != null) put("quantity", task.quantity)
            if (task.unit != null) put("unit", task.unit)
            if (task.inventoryItemId != null) put("inventoryItemId", task.inventoryItemId)
            if (task.assignedPeerId != null) put("assignedPeerId", task.assignedPeerId)
            if (task.latitude != null) put("lat", task.latitude)
            if (task.longitude != null) put("lng", task.longitude)

            val checklistArray = JSONArray()
            task.checklist.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("text", item.text)
                itemObj.put("checked", item.isChecked)
                if (item.quantity != null) itemObj.put("q", item.quantity)
                if (item.unit != null) itemObj.put("u", item.unit)
                checklistArray.put(itemObj)
            }
            put("checklist", checklistArray)
        }

        sendToPeer(peer, "TASK", taskJson.toString())
        
        chatDao.insertMessage(ChatMessageEntity(
            peerId = peerId,
            message = "📤 MISSION_SYNCED: ${task.title}",
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        ))
    }

    suspend fun shareInventoryItem(peerId: String, item: InventoryEntity) {
        val peer = chatDao.getPeerById(peerId) ?: return
        
        val json = JSONObject().apply {
            put("name", item.name)
            put("category", item.category)
            put("currentQuantity", item.currentQuantity)
            put("minQuantity", item.minQuantity)
            put("unit", item.unit)
            put("updatedAt", item.updatedAt)
        }

        sendToPeer(peer, "INVENTORY", json.toString())

        chatDao.insertMessage(ChatMessageEntity(
            peerId = peerId,
            message = "📤 INVENTORY_SHARED: ${item.name}",
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        ))
    }

    private fun sendToPeer(peer: PeerEntity, header: String, content: String) {
        scope.launch(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(peer.lastKnownIp, HINDRAX_PORT), 4000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(header)
                    writer.println(HindraxProfileCodec.encodePairingIdentity(myDeviceId, deviceIdManager.getNickname()))
                    writer.println(content)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun addManualPeer(deviceId: String, ip: String, nickname: String? = null) {
        if (!deviceId.startsWith("HNDX-")) return
        val existing = chatDao.getPeerById(deviceId)
        val normalizedNickname = nickname?.trim()?.takeIf { it.isNotBlank() }
        val peer = PeerEntity(
            id = deviceId,
            name = existing?.name ?: "Node_${deviceId.takeLast(4)}",
            nickname = existing?.nickname ?: normalizedNickname,
            lastKnownIp = ip,
            lastSeen = System.currentTimeMillis(),
            isOnline = true,
            latitude = existing?.latitude,
            longitude = existing?.longitude,
            locationAccuracy = existing?.locationAccuracy,
            locationUpdatedAt = existing?.locationUpdatedAt
        )
        chatDao.insertPeer(peer)
        if (existing == null) {
            notificationCenter.notifyPairing(peer.displayName, "IP: $ip")
        }
    }

    suspend fun sendMessage(peerId: String, text: String) {
        chatDao.insertMessage(ChatMessageEntity(peerId = peerId, message = text, timestamp = System.currentTimeMillis(), isFromMe = true))
        val peer = chatDao.getPeerById(peerId) ?: return
        sendToPeer(peer, "MESSAGE", text)
    }

    private suspend fun getOwnLocation(): DeviceLocationSnapshot {
        ensureLocationPermission()

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider ->
                try {
                    locationManager.isProviderEnabled(provider)
                } catch (e: Exception) {
                    false
                }
            }

        val lastKnown = providers
            .mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }

        if (lastKnown != null) return lastKnown.toSnapshot()

        val provider = providers.firstOrNull() ?: error("GPS/Network location provider disabled")
        return requestSingleLocation(provider)
    }

    private fun ensureLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            error("Location permission not granted")
        }
    }

    private suspend fun requestSingleLocation(provider: String): DeviceLocationSnapshot =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location.toSnapshot())
                }

                @Deprecated("Deprecated in Android SDK")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            try {
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            } catch (e: IllegalArgumentException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }

    private fun Location.toSnapshot(): DeviceLocationSnapshot {
        return DeviceLocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            accuracy = if (hasAccuracy()) accuracy else null,
            capturedAt = if (time > 0L) time else System.currentTimeMillis()
        )
    }

    private fun displayName(peerId: String, nickname: String?): String {
        return nickname?.takeIf { it.isNotBlank() } ?: "Node_${peerId.takeLast(4)}"
    }
}
