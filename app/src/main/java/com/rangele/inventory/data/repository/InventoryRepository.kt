package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeProducts(
        query: String = "",
        category: String? = null,
        sortByExpiration: Boolean = false,
    ): Flow<List<ProductEntity>>

    /** Products whose quantity has dropped below their own [ProductEntity.lowStockThreshold]. */
    fun observeLowStockProducts(): Flow<List<ProductEntity>>

    suspend fun getAllOnce(): List<ProductEntity>

    suspend fun getById(id: Long): ProductEntity?

    /** Returns an existing product likely to be the same item as [name], if any. */
    suspend fun findPotentialMatch(name: String): ProductEntity?

    /** Returns the product already carrying this exact [barcode], if any. */
    suspend fun findByBarcode(barcode: String): ProductEntity?

    /** Creates a brand new row, ignoring any existing similar product. */
    suspend fun insertAsNew(
        name: String,
        quantity: Double,
        unit: QuantityUnit,
        expirationDate: Long? = null,
        category: String? = null,
        lowStockThreshold: Double? = null,
        opened: Boolean = false,
        barcode: String? = null,
    ): Long

    /** Adds [quantity] to an already-existing product's stock. */
    suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
    )

    /** Updates an existing product's expiration date and opened status. */
    suspend fun updateDetails(
        productId: Long,
        expirationDate: Long?,
        opened: Boolean,
    )

    suspend fun setQuantity(
        productId: Long,
        quantity: Double,
    )

    suspend fun adjustQuantity(
        productId: Long,
        delta: Double,
    )

    suspend fun deleteProduct(productId: Long)
}
