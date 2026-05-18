package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.AuditSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditSessionDao {
    @Query("SELECT * FROM audit_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<AuditSessionEntity>>

    @Query("SELECT * FROM audit_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): AuditSessionEntity?

    @Insert
    suspend fun insertSession(session: AuditSessionEntity): Long

    @Update
    suspend fun updateSession(session: AuditSessionEntity)

    @Delete
    suspend fun deleteSession(session: AuditSessionEntity)
}
