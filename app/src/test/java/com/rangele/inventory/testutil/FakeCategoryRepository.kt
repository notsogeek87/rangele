package com.rangele.inventory.testutil

import com.rangele.inventory.data.local.entity.CategoryEntity
import com.rangele.inventory.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for [CategoryRepository]. */
class FakeCategoryRepository(
    initialCategories: List<CategoryEntity> = emptyList(),
) : CategoryRepository {
    private var nextId = (initialCategories.maxOfOrNull { it.id } ?: 0L) + 1
    private val categories = MutableStateFlow(initialCategories)

    override fun observeCategories(): Flow<List<CategoryEntity>> = categories

    override suspend fun createCategory(name: String): Long {
        val id = nextId++
        categories.value = categories.value + CategoryEntity(id = id, name = name.trim())
        return id
    }

    override suspend fun renameCategory(
        category: CategoryEntity,
        newName: String,
    ) {
        categories.value =
            categories.value.map { if (it.id == category.id) it.copy(name = newName.trim()) else it }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        categories.value = categories.value.filterNot { it.id == category.id }
    }
}
