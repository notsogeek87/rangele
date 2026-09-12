package com.rangele.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryEntryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<HistoryEntryEntity>

    @Insert
    suspend fun insert(entry: HistoryEntryEntity)
}
