package com.hindrax.ss.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.data.db.ChatDao
import com.hindrax.ss.data.db.InventoryDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.InventoryEntity
import com.hindrax.ss.data.entity.PeerEntity
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.domain.tasks.model.ChecklistItem
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
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

@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val deviceIdManager: DeviceIdManager
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val myDeviceId = deviceIdManager.getDeviceId()
    private val HINDRAX_PORT = 9999
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observePeers(): Flow<List<PeerEntity>> = chatDao.observePeers()
    fun observeMessages(peerId: String): Flow<List<ChatMessageEntity>> = chatDao.observeMessages(peerId)
    suspend fun getPeerById(id: String): PeerEntity? = chatDao.getPeerById(id)

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

                val header = reader.readLine() // IDENTITY, MESSAGE, TASK, or INVENTORY
                val remoteId = reader.readLine()
                val remoteIp = socket.inetAddress.hostAddress ?: ""

                when (header) {
                    "IDENTITY" -> {
                        writer.println(myDeviceId)
                        addManualPeer(remoteId, remoteIp)
                    }
                    "MESSAGE" -> {
                        val text = reader.readLine()
                        chatDao.insertMessage(ChatMessageEntity(
                            peerId = remoteId,
                            message = text,
                            timestamp = System.currentTimeMillis(),
                            isFromMe = false
                        ))
                        addManualPeer(remoteId, remoteIp)
                    }
                    "TASK" -> {
                        val taskJson = reader.readLine()
                        receiveSharedTask(taskJson, remoteId)
                        addManualPeer(remoteId, remoteIp)
                    }
                    "INVENTORY" -> {
                        val inventoryJson = reader.readLine()
                        receiveSharedInventory(inventoryJson, remoteId)
                        addManualPeer(remoteId, remoteIp)
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
                        unit = item.optString("u", null)
                    ))
                }
            }
            val incomingStatus = TaskStatus.valueOf(json.optString("status", TaskStatus.PENDIENTE.name))

            val receivedTitle: String
            if (existing != null) {
                // Update existing task's status and checklist
                val updated = existing.copy(
                    status = incomingStatus,
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
                    locationName = json.optString("location", null),
                    latitude = if (json.has("lat")) json.getDouble("lat") else null,
                    longitude = if (json.has("lng")) json.getDouble("lng") else null,
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
            val item = InventoryEntity(
                id = existing?.id ?: 0,
                name = name,
                category = json.getString("category"),
                currentQuantity = json.getDouble("currentQuantity"),
                minQuantity = json.getDouble("minQuantity"),
                unit = json.getString("unit"),
                updatedAt = System.currentTimeMillis()
            )
            
            inventoryDao.insert(item)
            
            chatDao.insertMessage(ChatMessageEntity(
                peerId = fromPeerId,
                message = "🛠️ INVENTORY_SYNCED: ${item.name}",
                timestamp = System.currentTimeMillis(),
                isFromMe = false
            ))
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun syncAllWithPeer(peerId: String) {
        val tasks = taskDao.getAllTasksSync()
        tasks.forEach { shareTask(peerId, it) }
        
        val inventory = inventoryDao.getAllInventorySync()
        inventory.forEach { shareInventoryItem(peerId, it) }
        
        chatDao.insertMessage(ChatMessageEntity(
            peerId = peerId,
            message = "🔄 FULL_FAMILY_SYNC_COMPLETED",
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        ))
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
            put("location", task.locationName)
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
                    writer.println(myDeviceId)
                    writer.println(content)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun addManualPeer(deviceId: String, ip: String) {
        if (!deviceId.startsWith("HNDX-")) return
        val peer = PeerEntity(
            id = deviceId,
            name = "Node_${deviceId.takeLast(4)}",
            lastKnownIp = ip,
            lastSeen = System.currentTimeMillis(),
            isOnline = true
        )
        chatDao.insertPeer(peer)
    }

    suspend fun sendMessage(peerId: String, text: String) {
        chatDao.insertMessage(ChatMessageEntity(peerId = peerId, message = text, timestamp = System.currentTimeMillis(), isFromMe = true))
        val peer = chatDao.getPeerById(peerId) ?: return
        sendToPeer(peer, "MESSAGE", text)
    }
}
