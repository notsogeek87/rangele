package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One physical unit of a product tracked in a discrete quantity unit (pièce/paquet, see
 * [com.rangele.inventory.data.model.QuantityUnit.step]), carrying its own optional expiration
 * date. Continuous units (poids/volume) never get rows here: [ProductEntity.expirationDate]
 * alone is used for them, exactly as before per-item dates existed.
 *
 * No SQL foreign key to `products`: this app never declares one (see [ProductEntity.category]/
 * [ProductEntity.pantryId]), so rows are deleted manually alongside their product instead.
 */
@Entity(tableName = "product_items", indices = [Index("product_id")])
data class ProductItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Long? = null,
    /**
     * Whether this specific unit has already been opened/started.
     *
     * The SQL default is declared so a migrated column matches the table Room creates on a fresh
     * install (see [ProductEntity.opened], which predates per-item tracking, for the same pattern).
     */
    @ColumnInfo(defaultValue = "0")
    val opened: Boolean = false,
)
