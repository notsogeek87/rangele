package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rangele.inventory.data.model.QuantityUnit

@Entity(tableName = "products", indices = [Index("barcode")])
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
    /**
     * Whether the product has already been opened/started.
     *
     * The SQL default is declared so a migrated column (`ALTER TABLE ... ADD COLUMN ... NOT NULL
     * DEFAULT 0`, which SQLite requires) matches the table Room creates on a fresh install.
     */
    @ColumnInfo(defaultValue = "0")
    val opened: Boolean = false,
    /** Code-barres scanné (EAN/UPC), utilisé pour reconnaître le produit lors d'un futur scan. */
    val barcode: String? = null,
    /** Placard du produit (voir [com.rangele.inventory.data.local.entity.PantryEntity]), null si non assigné. */
    @ColumnInfo(name = "pantry_id")
    val pantryId: Long? = null,
) {
    val quantityUnit: QuantityUnit
        get() = QuantityUnit.fromStorageValue(unit)
}
