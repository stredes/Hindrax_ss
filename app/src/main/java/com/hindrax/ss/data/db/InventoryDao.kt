package com.hindrax.ss.data.db

import androidx.room.*
import com.hindrax.ss.data.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY name ASC")
    fun observeInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventorySync(): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getById(id: Long): InventoryEntity?

    @Query("SELECT * FROM inventory WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryEntity): Long

    @Update
    suspend fun update(item: InventoryEntity)

    @Delete
    suspend fun delete(item: InventoryEntity)

    @Query("UPDATE inventory SET currentQuantity = :newQuantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Double, updatedAt: Long)
}
