package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rangele.inventory.data.model.QuantityUnit

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    /** Epoch millis (UTC midnight of the expiration day), null when not tracked. */
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Long? = null,
    /** Free-form category name (see [com.rangele.inventory.data.local.entity.CategoryEntity]), null when unassigned. */
    val category: String? = null,
    /** Below this quantity the product is surfaced in the suggested shopping list; null disables it. */
    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Double? = null,
    /** Whether the product has already been opened/started. */
    val opened: Boolean = false,
) {
    val quantityUnit: QuantityUnit
        get() = QuantityUnit.fromStorageValue(unit)
}
