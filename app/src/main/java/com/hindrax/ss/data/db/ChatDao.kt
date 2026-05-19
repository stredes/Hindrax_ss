package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.ChatMessageEntity
import com.hindrax.ss.data.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun observePeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    suspend fun getAllPeersSync(): List<PeerEntity>
    @Query("SELECT * FROM peers WHERE id = :id LIMIT 1")
    suspend fun getPeerById(id: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Query("UPDATE peers SET isOnline = :isOnline, lastSeen = :timestamp WHERE id = :id")
    suspend fun updatePeerStatus(id: String, isOnline: Boolean, timestamp: Long)

    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId ORDER BY timestamp ASC")
    fun observeMessages(peerId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE peerId = :peerId")
    suspend fun deleteMessagesWithPeer(peerId: String)
}
