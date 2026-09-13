package com.rangele.inventory.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rangele.inventory.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query(
        "SELECT * FROM products " +
            "WHERE (:query = '' OR name LIKE '%' || :query || '%') " +
            "AND (:category IS NULL OR category = :category) " +
            "ORDER BY " +
            "CASE WHEN :sortByExpiration THEN (expiration_date IS NULL) ELSE 0 END ASC, " +
            "CASE WHEN :sortByExpiration THEN expiration_date END ASC, " +
            "name COLLATE NOCASE ASC",
    )
    fun observeProducts(
        query: String,
        category: String?,
        sortByExpiration: Boolean,
    ): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query(
        "SELECT * FROM products " +
            "WHERE low_stock_threshold IS NOT NULL AND quantity < low_stock_threshold " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("UPDATE products SET category = NULL WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("UPDATE products SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(
        oldName: String,
        newName: String,
    )

    @Query("UPDATE products SET pantry_id = NULL WHERE pantry_id = :pantryId")
    suspend fun clearPantry(pantryId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(product: ProductEntity)

    /** Used when restoring a backup: replaces the whole table with [products] (see BackupRepository). */
    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(products: List<ProductEntity>)
}
