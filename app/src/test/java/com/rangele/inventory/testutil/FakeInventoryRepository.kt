package com.rangele.inventory.testutil

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.util.ProductNameMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.max

/** In-memory stand-in for [InventoryRepository], used to unit test ViewModels without Room. */
class FakeInventoryRepository(
    initialProducts: List<ProductEntity> = emptyList(),
) : InventoryRepository {
    private var nextId = (initialProducts.maxOfOrNull { it.id } ?: 0L) + 1
    private val products = MutableStateFlow(initialProducts)

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
                    threshold != null && product.quantity < threshold
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
        return id
    }

    override suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
    ) {
        replace(productId) { it.copy(quantity = it.quantity + addedQuantity) }
    }

    override suspend fun setQuantity(
        productId: Long,
        quantity: Double,
    ) {
        val newQuantity = max(0.0, quantity)
        if (newQuantity == 0.0) {
            products.value = products.value.filterNot { it.id == productId }
        } else {
            replace(productId) { it.copy(quantity = newQuantity) }
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

    override suspend fun deleteProduct(productId: Long) {
        products.value = products.value.filterNot { it.id == productId }
    }

    private fun replace(
        productId: Long,
        transform: (ProductEntity) -> ProductEntity,
    ) {
        products.value = products.value.map { if (it.id == productId) transform(it) else it }
    }
}
