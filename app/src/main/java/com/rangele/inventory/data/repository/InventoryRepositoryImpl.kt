package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.dao.HistoryEntryDao
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.dao.ProductItemDao
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.local.entity.ProductItemEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.util.ProductNameMatcher
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.roundToInt

class InventoryRepositoryImpl(
    private val productDao: ProductDao,
    private val historyEntryDao: HistoryEntryDao,
    private val productItemDao: ProductItemDao,
) : InventoryRepository {
    override fun observeProducts(
        query: String,
        category: String?,
        sortByExpiration: Boolean,
    ): Flow<List<ProductEntity>> = productDao.observeProducts(query.trim(), category, sortByExpiration)

    override fun observeLowStockProducts(): Flow<List<ProductEntity>> = productDao.observeLowStock()

    override suspend fun getAllOnce(): List<ProductEntity> = productDao.getAllOnce()

    override suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)

    override suspend fun findPotentialMatch(name: String): ProductEntity? {
        if (name.isBlank()) return null
        return ProductNameMatcher.findBestMatch(name, productDao.getAllOnce()) { it.name }
    }

    override suspend fun findByBarcode(barcode: String): ProductEntity? {
        if (barcode.isBlank()) return null
        return productDao.getByBarcode(barcode)
    }

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
        val id =
            productDao.insert(
                ProductEntity(
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.name,
                    expirationDate = expirationDate,
                    category = category,
                    lowStockThreshold = lowStockThreshold,
                    opened = opened,
                    barcode = barcode,
                    pantryId = pantryId,
                ),
            )
        if (unit.tracksItems) {
            val itemCount = quantity.roundToInt().coerceAtLeast(0)
            productItemDao.insertAll(
                List(itemCount) { ProductItemEntity(productId = id, expirationDate = expirationDate, opened = opened) },
            )
        }
        return id
    }

    override suspend fun incrementExisting(
        productId: Long,
        addedQuantity: Double,
        expirationDate: Long?,
    ) {
        val existing = productDao.getById(productId) ?: return
        val newQuantity = existing.quantity + addedQuantity
        val summary =
            if (existing.quantityUnit.tracksItems) {
                syncItemsToQuantity(productId, newQuantity, newItemExpirationDate = expirationDate)
            } else {
                ItemsSummary(existing.expirationDate, existing.opened)
            }
        productDao.update(
            existing.copy(
                quantity = newQuantity,
                expirationDate = summary.soonestExpirationDate,
                opened = summary.anyOpened,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setQuantity(
        productId: Long,
        quantity: Double,
    ) {
        val existing = productDao.getById(productId) ?: return
        val newQuantity = max(0.0, quantity)
        if (newQuantity < existing.quantity) {
            logWithdrawal(existing.name, existing.quantity - newQuantity, existing.unit)
        }
        val summary =
            if (existing.quantityUnit.tracksItems) {
                syncItemsToQuantity(productId, newQuantity)
            } else {
                ItemsSummary(existing.expirationDate, existing.opened)
            }
        productDao.update(
            existing.copy(
                quantity = newQuantity,
                expirationDate = summary.soonestExpirationDate,
                opened = summary.anyOpened,
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

    override suspend fun updateDetails(
        productId: Long,
        expirationDate: Long?,
        opened: Boolean,
    ) {
        val existing = productDao.getById(productId) ?: return
        productDao.update(
            existing.copy(
                expirationDate = expirationDate,
                opened = opened,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateLowStockThreshold(
        productId: Long,
        lowStockThreshold: Double?,
    ) {
        val existing = productDao.getById(productId) ?: return
        productDao.update(
            existing.copy(
                lowStockThreshold = lowStockThreshold,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteProduct(productId: Long) {
        val existing = productDao.getById(productId) ?: return
        logWithdrawal(existing.name, existing.quantity, existing.unit)
        productItemDao.deleteForProduct(productId)
        productDao.deleteById(productId)
    }

    override suspend fun getItems(productId: Long): List<ItemDetails> =
        productItemDao.getForProduct(productId).map { ItemDetails(it.expirationDate, it.opened) }

    override suspend fun saveItems(
        productId: Long,
        items: List<ItemDetails>,
    ) {
        val existing = productDao.getById(productId) ?: return
        val newQuantity = items.size.toDouble()
        if (newQuantity < existing.quantity) {
            logWithdrawal(existing.name, existing.quantity - newQuantity, existing.unit)
        }
        productItemDao.replaceForProduct(
            productId,
            items.map {
                ProductItemEntity(productId = productId, expirationDate = it.expirationDate, opened = it.opened)
            },
        )
        productDao.update(
            existing.copy(
                quantity = newQuantity,
                expirationDate = items.mapNotNull { it.expirationDate }.minOrNull(),
                opened = items.any { it.opened },
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun logWithdrawal(
        productName: String,
        quantityRemoved: Double,
        unit: String,
    ) {
        historyEntryDao.insert(
            HistoryEntryEntity(
                productName = productName,
                quantityRemoved = quantityRemoved,
                unit = unit,
            ),
        )
    }

    /**
     * Adds or removes item rows so their count matches [targetQuantity], then returns the new
     * product-level caches (see [ProductEntity.expirationDate]/[ProductEntity.opened]) for the
     * caller to persist. Added units get [newItemExpirationDate] and start unopened; removed
     * units are the soonest-expiring first (undated ones last), so a quick "-" tap consumes the
     * item closest to spoiling.
     */
    private suspend fun syncItemsToQuantity(
        productId: Long,
        targetQuantity: Double,
        newItemExpirationDate: Long? = null,
    ): ItemsSummary {
        val currentItems = productItemDao.getForProduct(productId)
        val targetCount = targetQuantity.roundToInt().coerceAtLeast(0)
        when {
            targetCount > currentItems.size ->
                productItemDao.insertAll(
                    List(targetCount - currentItems.size) {
                        ProductItemEntity(productId = productId, expirationDate = newItemExpirationDate)
                    },
                )
            targetCount < currentItems.size -> {
                val removalOrder =
                    currentItems.sortedWith(compareBy({ it.expirationDate == null }, { it.expirationDate }))
                productItemDao.delete(removalOrder.take(currentItems.size - targetCount))
            }
        }
        val finalItems = productItemDao.getForProduct(productId)
        return ItemsSummary(
            soonestExpirationDate = finalItems.mapNotNull { it.expirationDate }.minOrNull(),
            anyOpened = finalItems.any { it.opened },
        )
    }

    private data class ItemsSummary(
        val soonestExpirationDate: Long?,
        val anyOpened: Boolean,
    )
}

/** Whether a product in this unit is tracked as individual units with their own expiration date. */
private val QuantityUnit.tracksItems: Boolean get() = step == 1.0
