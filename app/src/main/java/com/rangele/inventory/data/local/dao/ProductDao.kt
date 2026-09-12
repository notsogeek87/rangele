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
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(product: ProductEntity)
}
