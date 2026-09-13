package com.rangele.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rangele.inventory.data.local.entity.PantryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantries ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PantryEntity>>

    @Query("SELECT * FROM pantries ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<PantryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pantry: PantryEntity): Long

    @Update
    suspend fun update(pantry: PantryEntity)

    @Query("DELETE FROM pantries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pantries SET is_default = 0")
    suspend fun clearDefault()

    @Query("UPDATE pantries SET is_default = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)
}
