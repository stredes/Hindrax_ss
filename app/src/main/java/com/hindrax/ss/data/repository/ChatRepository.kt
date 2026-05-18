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
import com.hindrax.ss.domain.tasks.model.TaskStatus
import com.hindrax.ss.domain.tasks.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
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
            val task = TaskEntity(
                title = "[SHARED] ${json.getString("title")}",
                description = "From: $fromPeerId\n\n${json.getString("description")}",
                status = TaskStatus.valueOf(json.optString("status", TaskStatus.PENDIENTE.name)),
                type = TaskType.valueOf(json.optString("type", TaskType.GENERAL.name)),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            taskDao.insert(task)
            
            chatDao.insertMessage(ChatMessageEntity(
                peerId = fromPeerId,
                message = "📦 Has recibido una nueva misión: ${task.title}",
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
            put("status", task.status.name)
            put("type", task.type.name)
        }

        scope.launch(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(peer.lastKnownIp, HINDRAX_PORT), 3000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println("TASK")
                    writer.println(myDeviceId)
                    writer.println(taskJson.toString())
                }
                chatDao.insertMessage(ChatMessageEntity(
                    peerId = peerId,
                    message = "📤 Has compartido la misión: ${task.title}",
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
