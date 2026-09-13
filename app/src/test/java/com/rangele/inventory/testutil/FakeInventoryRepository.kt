package com.rangele.inventory.testutil

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.ItemDetails
import com.rangele.inventory.util.ProductNameMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.roundToInt

/** In-memory stand-in for [InventoryRepository], used to unit test ViewModels without Room. */
class FakeInventoryRepository(
    initialProducts: List<ProductEntity> = emptyList(),
) : InventoryRepository {
    private var nextId = (initialProducts.maxOfOrNull { it.id } ?: 0L) + 1
    private val products = MutableStateFlow(initialProducts)

    /** Items per product id, mirroring `product_items` for discrete-unit products only. */
    private val items =
        initialProducts
            .filter { it.quantityUnit.tracksItems }
            .associateTo(mutableMapOf()) { product ->
                val item = ItemDetails(product.expirationDate, product.opened)
                product.id to MutableList(product.quantity.roundToInt().coerceAtLeast(0)) { item }
            }

    override fun observeProducts(
        query: String,
        category: String?,
        sortByExpiration: Boolean,
    ): Flow<List<ProductEntity>> =
        products.map { list ->
            val filtered =
                list
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .filter { category == null || it.category == category }
            if (sortByExpiration) {
                filtered.sortedWith(
                    compareBy<ProductEntity> { it.expirationDate == null }
                        .thenBy { it.expirationDate }
                        .thenBy { it.name.lowercase() },
                )
            } else {
                filtered.sortedBy { it.name.lowercase() }
            }
        }

    override fun observeLowStockProducts(): Flow<List<ProductEntity>> =
        products.map { list ->
            list
                .filter { product ->
                    val threshold = product.lowStockThreshold
                    product.inShoppingList || (threshold != null && product.quantity <= threshold)
                }.sortedBy { it.name.lowercase() }
        }

    override suspend fun getAllOnce(): List<ProductEntity> = products.value

    override suspend fun getById(id: Long): ProductEntity? = products.value.firstOrNull { it.id == id }

    override suspend fun findPotentialMatch(name: String): ProductEntity? =
        ProductNameMatcher.findBestMatch(name, products.value) { it.name }

    override suspend fun findByBarcode(barcode: String): ProductEntity? =
        products.value.firstOrNull { it.barcode == barcode }

    override suspend fun insertAsNew(
        name: String,
        quantity: Double,
        unit: QuantityUnit,
        expirationDate: Long?,
        category: String?,
        lowStockThreshold: Double?,
        opened: Boolean,
        barcode: String?,
        pantryId: Long?,
    ): Long {
        val id = nextId++
        products.value = products.value +
            ProductEntity(
                id = id,
                name = name,
                quantity = quantity,
                unit = unit.name,
                expirationDate = expirationDate,
                category = category,
                lowStockThreshold = lowStockThreshold,
                opened = opened,
                barcode = barcode,
                pantryId = pantryId,
            )
        if (unit.tracksItems) {
            val item = ItemDetails(expirationDate, opened)
            items[id] = MutableList(quantity.roundToInt().coerceAtLeast(0)) { item }
        }
        return id
    }

    override suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
        expirationDate: Long?,
    ) {
        val existing = getById(productId) ?: return
        val newQuantity = existing.quantity + addedQuantity
        val summary =
            if (existing.quantityUnit.tracksItems) {
                syncItemsToQuantity(productId, newQuantity, newItemExpirationDate = expirationDate)
            } else {
                ItemsSummary(existing.expirationDate, existing.opened)
            }
        replace(productId) {
            it.copy(quantity = newQuantity, expirationDate = summary.soonestExpirationDate, opened = summary.anyOpened)
        }
    }

    override suspend fun setQuantity(
        productId: Long,
        quantity: Double,
    ) {
        val existing = getById(productId) ?: return
        val newQuantity = max(0.0, quantity)
        val summary =
            if (existing.quantityUnit.tracksItems) {
                syncItemsToQuantity(productId, newQuantity)
            } else {
                ItemsSummary(existing.expirationDate, existing.opened)
            }
        replace(productId) {
            it.copy(quantity = newQuantity, expirationDate = summary.soonestExpirationDate, opened = summary.anyOpened)
        }
    }

    override suspend fun adjustQuantity(
        productId: Long,
        delta: Double,
    ) {
        val existing = getById(productId) ?: return
        setQuantity(productId, existing.quantity + delta)
    }

    override suspend fun updateDetails(
        productId: Long,
        expirationDate: Long?,
        opened: Boolean,
    ) {
        replace(productId) { it.copy(expirationDate = expirationDate, opened = opened) }
    }

    override suspend fun updateLowStockThreshold(
        productId: Long,
        lowStockThreshold: Double?,
    ) {
        replace(productId) { it.copy(lowStockThreshold = lowStockThreshold) }
    }

    override suspend fun setInShoppingList(
        productId: Long,
        inShoppingList: Boolean,
    ) {
        replace(productId) { it.copy(inShoppingList = inShoppingList) }
    }

    override suspend fun deleteProduct(productId: Long) {
        items.remove(productId)
        products.value = products.value.filterNot { it.id == productId }
    }

    override suspend fun getItems(productId: Long): List<ItemDetails> = items[productId]?.toList() ?: emptyList()

    override suspend fun saveItems(
        productId: Long,
        items: List<ItemDetails>,
    ) {
        getById(productId) ?: return
        val newQuantity = items.size.toDouble()
        this.items[productId] = items.toMutableList()
        replace(productId) {
            it.copy(
                quantity = newQuantity,
                expirationDate = items.mapNotNull { item -> item.expirationDate }.minOrNull(),
                opened = items.any { item -> item.opened },
            )
        }
    }

    /** Mirrors [com.rangele.inventory.data.repository.InventoryRepositoryImpl]'s FEFO sync. */
    private fun syncItemsToQuantity(
        productId: Long,
        targetQuantity: Double,
        newItemExpirationDate: Long? = null,
    ): ItemsSummary {
        val current = items.getOrPut(productId) { mutableListOf() }
        val targetCount = targetQuantity.roundToInt().coerceAtLeast(0)
        when {
            targetCount > current.size ->
                repeat(targetCount - current.size) { current.add(ItemDetails(newItemExpirationDate, opened = false)) }
            targetCount < current.size -> {
                val removalOrder =
                    current.sortedWith(compareBy({ it.expirationDate == null }, { it.expirationDate }))
                removalOrder.take(current.size - targetCount).forEach { current.remove(it) }
            }
        }
        return ItemsSummary(
            soonestExpirationDate = current.mapNotNull { it.expirationDate }.minOrNull(),
            anyOpened = current.any { it.opened },
        )
    }

    private data class ItemsSummary(
        val soonestExpirationDate: Long?,
        val anyOpened: Boolean,
    )

    private fun replace(
        productId: Long,
        transform: (ProductEntity) -> ProductEntity,
    ) {
        products.value = products.value.map { if (it.id == productId) transform(it) else it }
    }
}

/** Mirrors [com.rangele.inventory.data.repository.InventoryRepositoryImpl]'s per-item tracking rule. */
private val QuantityUnit.tracksItems: Boolean get() = step == 1.0
