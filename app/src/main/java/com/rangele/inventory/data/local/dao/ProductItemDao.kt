package com.rangele.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rangele.inventory.data.local.entity.ProductItemEntity

@Dao
interface ProductItemDao {
    @Query("SELECT * FROM product_items WHERE product_id = :productId")
    suspend fun getForProduct(productId: Long): List<ProductItemEntity>

    /** Used for backup export (see BackupRepository). */
    @Query("SELECT * FROM product_items")
    suspend fun getAllOnce(): List<ProductItemEntity>

    @Insert
    suspend fun insertAll(items: List<ProductItemEntity>)

    @Delete
    suspend fun delete(items: List<ProductItemEntity>)

    @Query("DELETE FROM product_items WHERE product_id = :productId")
    suspend fun deleteForProduct(productId: Long)

    /** Replaces every item of [productId] with one row per entry of [expirationDates], in order. */
    @Transaction
    suspend fun replaceForProduct(
        productId: Long,
        expirationDates: List<Long?>,
    ) {
        deleteForProduct(productId)
        insertAll(expirationDates.map { ProductItemEntity(productId = productId, expirationDate = it) })
    }

    /** Used when restoring a backup: replaces the whole table with the imported rows (see BackupRepository). */
    @Query("DELETE FROM product_items")
    suspend fun deleteAll()
}
