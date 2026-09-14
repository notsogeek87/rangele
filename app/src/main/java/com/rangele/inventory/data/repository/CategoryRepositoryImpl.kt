package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.dao.CategoryDao
import com.rangele.inventory.data.local.dao.ProductDao
import com.rangele.inventory.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
) : CategoryRepository {
    override fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    override suspend fun ensureCategory(name: String): CategoryEntity {
        val trimmed = name.trim()
        categoryDao.getByName(trimmed)?.let { return it }
        val id = categoryDao.insert(CategoryEntity(name = trimmed))
        return CategoryEntity(id = id, name = trimmed)
    }

    override suspend fun renameCategory(
        category: CategoryEntity,
        newName: String,
    ) {
        val trimmed = newName.trim()
        productDao.renameCategory(category.name, trimmed)
        categoryDao.update(category.copy(name = trimmed))
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        productDao.clearCategory(category.name)
        categoryDao.deleteById(category.id)
    }
}
