package com.hindrax.ss.data.repository

import com.hindrax.ss.data.db.ToolTaskDao
import com.hindrax.ss.data.entity.ToolTaskEntity
import kotlinx.coroutines.flow.Flow

// Renombrado para evitar conflicto con el sistema de misiones
class ToolRepository(private val toolTaskDao: ToolTaskDao) {
    val enabledTasks: Flow<List<ToolTaskEntity>> = toolTaskDao.getEnabledTasks()

    fun getTasksByCategory(category: String): Flow<List<ToolTaskEntity>> =
        toolTaskDao.getTasksByCategory(category)

    suspend fun insertTask(task: ToolTaskEntity) = toolTaskDao.insertTask(task)
}
