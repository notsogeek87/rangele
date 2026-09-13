package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pantries", indices = [Index(value = ["name"], unique = true)])
data class PantryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** At most one pantry is default at a time (see [com.rangele.inventory.data.repository.PantryRepository]). */
    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault: Boolean = false,
)
