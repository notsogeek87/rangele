package com.rangele.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rangele.inventory.data.local.entity.OffProductCacheEntity

@Dao
interface OffProductCacheDao {
    @Query("SELECT * FROM off_product_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): OffProductCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OffProductCacheEntity)
}
