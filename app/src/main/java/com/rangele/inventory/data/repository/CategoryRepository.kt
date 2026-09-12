package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<CategoryEntity>>

    suspend fun createCategory(name: String): Long

    suspend fun renameCategory(
        category: CategoryEntity,
        newName: String,
    )

    /** Deletes the category and detaches it from any product (product stays, category becomes null). */
    suspend fun deleteCategory(category: CategoryEntity)
}
