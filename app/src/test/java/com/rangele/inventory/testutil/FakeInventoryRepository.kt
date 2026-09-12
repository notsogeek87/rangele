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

    override fun observeProducts(query: String): Flow<List<ProductEntity>> =
        products.map { list ->
            list
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun getAllOnce(): List<ProductEntity> = products.value

    override suspend fun getById(id: Long): ProductEntity? = products.value.firstOrNull { it.id == id }

    override suspend fun findPotentialMatch(name: String): ProductEntity? =
        ProductNameMatcher.findBestMatch(name, products.value) { it.name }

    override suspend fun insertAsNew(
        name: String,
        quantity: Double,
        unit: QuantityUnit,
    ): Long {
        val id = nextId++
        products.value = products.value +
            ProductEntity(
                id = id,
                name = name,
                quantity = quantity,
                unit = unit.name,
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
        replace(productId) { it.copy(quantity = max(0.0, quantity)) }
    }

    override suspend fun adjustQuantity(
        productId: Long,
        delta: Double,
    ) {
        val existing = getById(productId) ?: return
        setQuantity(productId, existing.quantity + delta)
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
