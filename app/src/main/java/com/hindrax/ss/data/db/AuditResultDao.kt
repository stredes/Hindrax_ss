package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.AuditResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditResultDao {
    @Query("SELECT * FROM audit_results WHERE sessionId = :sessionId")
    fun getResultsForSession(sessionId: Long): Flow<List<AuditResultEntity>>

    @Insert
    suspend fun insertResult(result: AuditResultEntity): Long
}
