package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.dao.PantryDao
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.entity.PantryEntity
import kotlinx.coroutines.flow.Flow

class PantryRepositoryImpl(
    private val pantryDao: PantryDao,
    private val productDao: ProductDao,
) : PantryRepository {
    override fun observePantries(): Flow<List<PantryEntity>> = pantryDao.observeAll()

    override suspend fun createPantry(name: String): Long = pantryDao.insert(PantryEntity(name = name.trim()))

    override suspend fun renamePantry(
        pantry: PantryEntity,
        newName: String,
    ) {
        pantryDao.update(pantry.copy(name = newName.trim()))
    }

    override suspend fun deletePantry(pantry: PantryEntity) {
        productDao.clearPantry(pantry.id)
        pantryDao.deleteById(pantry.id)
    }

    override suspend fun setDefaultPantry(pantryId: Long?) {
        pantryDao.clearDefault()
        if (pantryId != null) pantryDao.markDefault(pantryId)
    }
}
