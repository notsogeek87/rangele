package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import kotlinx.coroutines.flow.Flow

/**
 * Seuil de stock bas attribué à tout nouveau produit, quel que soit le mode d'ajout (manuel, scan
 * de code-barres ou revue de ticket) : le produit est ainsi proposé en liste de courses dès qu'il
 * n'en reste plus qu'une unité, sans réglage préalable. Il reste modifiable — ou effaçable, ce qui
 * exclut définitivement le produit des suggestions — depuis sa fiche.
 */
const val DEFAULT_LOW_STOCK_THRESHOLD: Double = 1.0

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
        lowStockThreshold: Double? = DEFAULT_LOW_STOCK_THRESHOLD,
        opened: Boolean = false,
        barcode: String? = null,
        pantryId: Long? = null,
    ): Long

    /**
     * Adds [addedQuantity] to an already-existing product's stock. For a discrete-unit product
     * (see [QuantityUnit.step]) that already tracks per-item dates, the newly added units get
     * [expirationDate]; existing items are untouched.
     */
    suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
        expirationDate: Long? = null,
    )

    /** Updates an existing product's expiration date and opened status. Continuous units only. */
    suspend fun updateDetails(
        productId: Long,
        expirationDate: Long?,
        opened: Boolean,
    )

    /** Updates an existing product's low-stock threshold, editable from its product sheet regardless of unit kind. */
    suspend fun updateLowStockThreshold(
        productId: Long,
        lowStockThreshold: Double?,
    )

    /**
     * Ajoute ou retire manuellement le produit de la liste de courses suggérée, sans toucher à son
     * stock ni à son seuil : un produit bien approvisionné peut ainsi y figurer quand même.
     */
    suspend fun setInShoppingList(
        productId: Long,
        inShoppingList: Boolean,
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

    /**
     * One entry per unit in stock of a discrete-unit product, each with its own expiration date
     * and opened status. Empty for a continuous-unit product.
     */
    suspend fun getItems(productId: Long): List<ItemDetails>

    /**
     * Replaces a discrete-unit product's items with [items] (one entry per unit): its quantity
     * becomes `items.size`.
     */
    suspend fun saveItems(
        productId: Long,
        items: List<ItemDetails>,
    )
}

/** One unit in stock of a discrete-unit product (see [ProductEntity.quantityUnit]). */
data class ItemDetails(
    val expirationDate: Long?,
    val opened: Boolean,
)
