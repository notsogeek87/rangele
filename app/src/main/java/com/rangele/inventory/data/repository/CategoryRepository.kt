package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<CategoryEntity>>

    /**
     * Renvoie la catégorie [name] si elle existe déjà (comparaison insensible à la casse), sinon la crée.
     * Il n'y a plus de création manuelle dans l'app : les catégories proviennent uniquement d'Open Food
     * Facts au moment du scan d'un code-barres (voir BarcodeScanViewModel).
     */
    suspend fun ensureCategory(name: String): CategoryEntity

    suspend fun renameCategory(
        category: CategoryEntity,
        newName: String,
    )

    /** Deletes the category and detaches it from any product (product stays, category becomes null). */
    suspend fun deleteCategory(category: CategoryEntity)
}
