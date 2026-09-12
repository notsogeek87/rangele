package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.util.ProductNameMatcher
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class InventoryRepositoryImpl(
    private val productDao: ProductDao,
) : InventoryRepository {
    override fun observeProducts(query: String): Flow<List<ProductEntity>> = productDao.observeProducts(query.trim())

    override suspend fun getAllOnce(): List<ProductEntity> = productDao.getAllOnce()

    override suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)

    override suspend fun findPotentialMatch(name: String): ProductEntity? {
        if (name.isBlank()) return null
        return ProductNameMatcher.findBestMatch(name, productDao.getAllOnce()) { it.name }
    }

    override suspend fun insertAsNew(
        name: String,
        quantity: Double,
        unit: QuantityUnit,
    ): Long =
        productDao.insert(
            ProductEntity(
                name = name.trim(),
                quantity = quantity,
                unit = unit.name,
            ),
        )

    override suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
    ) {
        val existing = productDao.getById(productId) ?: return
        productDao.update(
            existing.copy(
                quantity = existing.quantity + addedQuantity,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setQuantity(
        productId: Long,
        quantity: Double,
    ) {
        val existing = productDao.getById(productId) ?: return
        productDao.update(
            existing.copy(
                quantity = max(0.0, quantity),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun adjustQuantity(
        productId: Long,
        delta: Double,
    ) {
        val existing = productDao.getById(productId) ?: return
        setQuantity(productId, existing.quantity + delta)
    }

    override suspend fun deleteProduct(productId: Long) {
        productDao.deleteById(productId)
    }
}
