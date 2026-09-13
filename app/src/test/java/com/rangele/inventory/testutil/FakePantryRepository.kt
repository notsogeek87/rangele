package com.rangele.inventory.testutil

import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for [PantryRepository]. */
class FakePantryRepository(
    initialPantries: List<PantryEntity> = emptyList(),
) : PantryRepository {
    private var nextId = (initialPantries.maxOfOrNull { it.id } ?: 0L) + 1
    private val pantries = MutableStateFlow(initialPantries)

    override fun observePantries(): Flow<List<PantryEntity>> = pantries

    override suspend fun createPantry(name: String): Long {
        val id = nextId++
        pantries.value = pantries.value + PantryEntity(id = id, name = name.trim())
        return id
    }

    override suspend fun renamePantry(
        pantry: PantryEntity,
        newName: String,
    ) {
        pantries.value = pantries.value.map { if (it.id == pantry.id) it.copy(name = newName.trim()) else it }
    }

    override suspend fun deletePantry(pantry: PantryEntity) {
        pantries.value = pantries.value.filterNot { it.id == pantry.id }
    }

    override suspend fun setDefaultPantry(pantryId: Long?) {
        pantries.value = pantries.value.map { it.copy(isDefault = it.id == pantryId) }
    }
}
