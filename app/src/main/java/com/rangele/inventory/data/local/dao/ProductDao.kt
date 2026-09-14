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
    /**
     * Les deux drapeaux de tri sont exclusifs (voir [com.rangele.inventory.ui.inventory.SortMode]) :
     * chaque `CASE` neutralise son critère quand il n'est pas demandé, et le tri par nom sert
     * toujours de départage final. `-created_at` donne le plus récemment ajouté en premier, sans
     * avoir besoin d'un `DESC` que le `CASE` ne pourrait pas rendre conditionnel.
     */
    @Query(
        "SELECT * FROM products " +
            "WHERE (:query = '' OR name LIKE '%' || :query || '%') " +
            "AND (:category IS NULL OR category = :category) " +
            "ORDER BY " +
            "CASE WHEN :sortByRecent THEN -created_at ELSE 0 END ASC, " +
            "CASE WHEN :sortByExpiration THEN (expiration_date IS NULL) ELSE 0 END ASC, " +
            "CASE WHEN :sortByExpiration THEN expiration_date END ASC, " +
            "name COLLATE NOCASE ASC",
    )
    fun observeProducts(
        query: String,
        category: String?,
        sortByExpiration: Boolean,
        sortByRecent: Boolean,
    ): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    /** Suggestion automatique (sous le seuil) ou ajout manuel : les deux alimentent la même liste. */
    @Query(
        "SELECT * FROM products " +
            "WHERE in_shopping_list = 1 " +
            "OR (low_stock_threshold IS NOT NULL AND quantity <= low_stock_threshold) " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("UPDATE products SET in_shopping_list = :inShoppingList WHERE id = :productId")
    suspend fun setInShoppingList(
        productId: Long,
        inShoppingList: Boolean,
    )

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
