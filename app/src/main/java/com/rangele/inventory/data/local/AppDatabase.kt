package com.rangele.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rangele.inventory.data.local.dao.CategoryDao
import com.rangele.inventory.data.local.dao.HistoryEntryDao
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.entity.CategoryEntity
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import com.rangele.inventory.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, CategoryEntity::class, HistoryEntryEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun categoryDao(): CategoryDao

    abstract fun historyEntryDao(): HistoryEntryDao

    companion object {
        const val DATABASE_NAME = "rangele.db"
    }
}
