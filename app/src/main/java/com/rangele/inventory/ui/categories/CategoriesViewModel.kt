package com.rangele.inventory.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.CategoryEntity
import com.rangele.inventory.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
)

class CategoriesViewModel(
    private val repository: CategoryRepository,
) : ViewModel() {
    val uiState: StateFlow<CategoriesUiState> =
        repository
            .observeCategories()
            .map { CategoriesUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onRenameCategory(
        category: CategoryEntity,
        newName: String,
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameCategory(category, newName) }
    }

    fun onDeleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
}
