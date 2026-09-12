package com.rangele.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        const val DATABASE_NAME = "rangele.db"
    }
}
