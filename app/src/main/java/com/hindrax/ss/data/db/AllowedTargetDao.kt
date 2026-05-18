package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.AllowedTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedTargetDao {
    @Query("SELECT * FROM allowed_targets ORDER BY createdAt DESC")
    fun getAllAllowedTargets(): Flow<List<AllowedTargetEntity>>

    @Query("SELECT * FROM allowed_targets WHERE targetValue = :value LIMIT 1")
    suspend fun getTargetByValue(value: String): AllowedTargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowedTarget(target: AllowedTargetEntity)

    @Delete
    suspend fun deleteAllowedTarget(target: AllowedTargetEntity)
}
