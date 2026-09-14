package com.rangele.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rangele.inventory.data.local.dao.CategoryDao
import com.rangele.inventory.data.local.dao.HistoryEntryDao
import com.rangele.inventory.data.local.dao.OffProductCacheDao
import com.rangele.inventory.data.local.dao.PantryDao
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.dao.ProductItemDao
import com.rangele.inventory.data.local.entity.CategoryEntity
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import com.rangele.inventory.data.local.entity.OffProductCacheEntity
import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.local.entity.ProductItemEntity

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        HistoryEntryEntity::class,
        PantryEntity::class,
        ProductItemEntity::class,
        OffProductCacheEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun categoryDao(): CategoryDao

    abstract fun historyEntryDao(): HistoryEntryDao

    abstract fun pantryDao(): PantryDao

    abstract fun productItemDao(): ProductItemDao

    abstract fun offProductCacheDao(): OffProductCacheDao

    companion object {
        const val DATABASE_NAME = "rangele.db"
    }
}
