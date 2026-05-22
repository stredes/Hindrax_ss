package com.hindrax.ss.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val id: String, // Este es el Hash único (HNDX-XXXX)
    val name: String,
    val nickname: String? = null,
    val lastKnownIp: String,
    val lastSeen: Long,
    val isOnline: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    val locationUpdatedAt: Long? = null
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() } ?: name

    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    val locationLabel: String
        get() = if (hasLocation) {
            String.format(java.util.Locale.US, "%.6f,%.6f", latitude, longitude)
        } else {
            "NO_GPS_FIX"
        }
}

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
