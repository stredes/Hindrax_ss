package com.hindrax.ss.data.repository

import com.hindrax.ss.data.db.AllowedTargetDao
import com.hindrax.ss.data.entity.AllowedTargetEntity
import kotlinx.coroutines.flow.Flow

class TargetRepository(private val allowedTargetDao: AllowedTargetDao) {
    fun getAllAllowedTargets(): Flow<List<AllowedTargetEntity>> = allowedTargetDao.getAllAllowedTargets()

    suspend fun getTargetByValue(value: String): AllowedTargetEntity? = allowedTargetDao.getTargetByValue(value)

    suspend fun addAllowedTarget(target: AllowedTargetEntity) = allowedTargetDao.insertAllowedTarget(target)

    suspend fun removeAllowedTarget(target: AllowedTargetEntity) = allowedTargetDao.deleteAllowedTarget(target)
}
