package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val id: String, // Este es el Hash único (HNDX-XXXX)
    val name: String,
    val lastKnownIp: String,
    val lastSeen: Long,
    val isOnline: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val peerId: String, // ID del Hash del destinatario
    val message: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, ERROR
}
