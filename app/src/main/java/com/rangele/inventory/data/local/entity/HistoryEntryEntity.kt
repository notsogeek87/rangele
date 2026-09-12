package com.rangele.inventory.data.local.entity

/** A simple, append-only journal of withdrawals/deletions — see [HistoryEntryDao]. */
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "product_name")
    val productName: String,
    /** Always positive: the amount removed from stock (or the full quantity, on deletion). */
    @ColumnInfo(name = "quantity_removed")
    val quantityRemoved: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
)
