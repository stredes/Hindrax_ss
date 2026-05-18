package com.hindrax.ss.data.repository

import com.hindrax.ss.data.db.AuditResultDao
import com.hindrax.ss.data.db.AuditSessionDao
import com.hindrax.ss.data.entity.AuditResultEntity
import com.hindrax.ss.data.entity.AuditSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuditRepository @Inject constructor(
    private val sessionDao: AuditSessionDao,
    private val resultDao: AuditResultDao
) {
    fun getAllSessions(): Flow<List<AuditSessionEntity>> = sessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): AuditSessionEntity? = sessionDao.getSessionById(id)

    suspend fun startSession(session: AuditSessionEntity): Long = sessionDao.insertSession(session)

    suspend fun updateSession(session: AuditSessionEntity) = sessionDao.updateSession(session)

    fun getResultsForSession(sessionId: Long): Flow<List<AuditResultEntity>> = 
        resultDao.getResultsForSession(sessionId)

    suspend fun saveResult(result: AuditResultEntity) = resultDao.insertResult(result)
}
