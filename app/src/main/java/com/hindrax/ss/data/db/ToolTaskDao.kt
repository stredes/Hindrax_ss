package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.ToolTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolTaskDao {
    @Query("SELECT * FROM tool_tasks WHERE enabled = 1")
    fun getEnabledTasks(): Flow<List<ToolTaskEntity>>

    @Query("SELECT * FROM tool_tasks WHERE category = :category AND enabled = 1")
    fun getTasksByCategory(category: String): Flow<List<ToolTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ToolTaskEntity)
}
