package com.rangele.inventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.repository.CategoryRepository
import com.rangele.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode {
    NAME,
    EXPIRATION,
}

data class InventoryUiState(
    val searchQuery: String = "",
    val products: List<ProductEntity> = emptyList(),
    val isLoading: Boolean = true,
    val sortMode: SortMode = SortMode.NAME,
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
)

private data class Filters(
    val query: String,
    val sortMode: SortMode,
    val category: String?,
    val availableCategories: List<String>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(SortMode.NAME)
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InventoryUiState> =
        combine(
            searchQuery,
            sortMode,
            selectedCategory,
            categoryRepository.observeCategories(),
        ) { query, sort, category, categories ->
            Filters(query, sort, category, categories.map { it.name })
        }.flatMapLatest { filters ->
            repository
                .observeProducts(
                    query = filters.query,
                    category = filters.category,
                    sortByExpiration = filters.sortMode == SortMode.EXPIRATION,
                ).map { products ->
                    InventoryUiState(
                        searchQuery = filters.query,
                        products = products,
                        isLoading = false,
                        sortMode = filters.sortMode,
                        selectedCategory = filters.category,
                        availableCategories = filters.availableCategories,
                    )
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InventoryUiState(),
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSortModeChanged(mode: SortMode) {
        sortMode.value = mode
    }

    fun onCategoryFilterChanged(category: String?) {
        selectedCategory.value = category
    }

    fun onIncrement(product: ProductEntity) {
        viewModelScope.launch { repository.adjustQuantity(product.id, product.quantityUnit.step) }
    }

    fun onDecrement(product: ProductEntity) {
        viewModelScope.launch { repository.adjustQuantity(product.id, -product.quantityUnit.step) }
    }

    fun onQuantitySet(
        product: ProductEntity,
        newQuantity: Double,
    ) {
        viewModelScope.launch { repository.setQuantity(product.id, newQuantity) }
    }

    fun onDetailsUpdated(
        product: ProductEntity,
        expirationDate: Long?,
        opened: Boolean,
    ) {
        viewModelScope.launch { repository.updateDetails(product.id, expirationDate, opened) }
    }

    fun onDelete(product: ProductEntity) {
        viewModelScope.launch { repository.deleteProduct(product.id) }
    }
}
