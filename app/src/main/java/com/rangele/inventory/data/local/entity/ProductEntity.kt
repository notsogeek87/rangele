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
) {
    val quantityUnit: QuantityUnit
        get() = QuantityUnit.fromStorageValue(unit)
}
