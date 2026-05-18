package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.TaskEntity
import com.hindrax.ss.data.entity.TaskHistoryEntity
import com.hindrax.ss.domain.tasks.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    fun observeTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus, updatedAt: Long)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Insert
    suspend fun insertHistory(history: TaskHistoryEntity)

    @Query("SELECT * FROM task_history WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun observeHistoryByTaskId(taskId: Long): Flow<List<TaskHistoryEntity>>
}
