package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.PantryEntity
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observePantries(): Flow<List<PantryEntity>>

    suspend fun createPantry(name: String): Long

    suspend fun renamePantry(
        pantry: PantryEntity,
        newName: String,
    )

    /** Deletes the pantry and detaches it from any product (product stays, pantry becomes null). */
    suspend fun deletePantry(pantry: PantryEntity)

    /** Sets [pantryId] as the sole default pantry preselected when adding a product, or clears it when null. */
    suspend fun setDefaultPantry(pantryId: Long?)
}
