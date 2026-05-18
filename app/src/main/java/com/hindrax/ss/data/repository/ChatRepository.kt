package com.hindrax.ss.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.hindrax.ss.core.util.DeviceIdManager
import com.hindrax.ss.data.db.ChatDao
import com.hindrax.ss.data.db.TaskDao
import com.hindrax.ss.data.entity.ChatMessageEntity
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

                val header = reader.readLine() // IDENTITY, MESSAGE, or TASK
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
                }
                socket.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun receiveSharedTask(jsonStr: String, fromPeerId: String) {
        try {
            val json = JSONObject(jsonStr)
            
            // Re-mapear Checklist
            val checklistJson = json.optJSONArray("checklist")
            val checklist = mutableListOf<ChecklistItem>()
            if (checklistJson != null) {
                for (i in 0 until checklistJson.length()) {
                    val item = checklistJson.getJSONObject(i)
                    checklist.add(ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        text = item.getString("text"),
                        isChecked = false, // Las tareas compartidas llegan limpias
                        quantity = if (item.has("q")) item.getDouble("q") else null,
                        unit = item.optString("u", null)
                    ))
                }
            }

            val task = TaskEntity(
                title = "[SHARED] ${json.getString("title")}",
                description = "Node Source: $fromPeerId\n\n${json.getString("description")}",
                status = TaskStatus.PENDIENTE,
                type = TaskType.valueOf(json.optString("type", TaskType.GENERAL.name)),
                locationName = json.optString("location", null),
                latitude = if (json.has("lat")) json.getDouble("lat") else null,
                longitude = if (json.has("lng")) json.getDouble("lng") else null,
                checklist = checklist,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            taskDao.insert(task)
            
            chatDao.insertMessage(ChatMessageEntity(
                peerId = fromPeerId,
                message = "📦 MISSION_RECEIVED: ${task.title}",
                timestamp = System.currentTimeMillis(),
                isFromMe = false
            ))
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun shareTask(peerId: String, task: TaskEntity) {
        val peer = chatDao.getPeerById(peerId) ?: return
        
        val taskJson = JSONObject().apply {
            put("title", task.title)
            put("description", task.description)
            put("type", task.type.name)
            put("location", task.locationName)
            if (task.latitude != null) put("lat", task.latitude)
            if (task.longitude != null) put("lng", task.longitude)
            
            val checklistArray = JSONArray()
            task.checklist.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("text", item.text)
                if (item.quantity != null) itemObj.put("q", item.quantity)
                if (item.unit != null) itemObj.put("u", item.unit)
                checklistArray.put(itemObj)
            }
            put("checklist", checklistArray)
        }

        scope.launch(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(peer.lastKnownIp, HINDRAX_PORT), 4000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println("TASK")
                    writer.println(myDeviceId)
                    writer.println(taskJson.toString())
                }
                chatDao.insertMessage(ChatMessageEntity(
                    peerId = peerId,
                    message = "📤 MISSION_SYNCED: ${task.title}",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true
                ))
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
        scope.launch(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(peer.lastKnownIp, HINDRAX_PORT), 2000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println("MESSAGE")
                    writer.println(myDeviceId)
                    writer.println(text)
                }
            } catch (e: Exception) { }
        }
    }
}
