package com.rangele.inventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val searchQuery: String = "",
    val products: List<ProductEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<InventoryUiState> =
        searchQuery
            .flatMapLatest { query ->
                repository.observeProducts(query).map { products ->
                    InventoryUiState(searchQuery = query, products = products, isLoading = false)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState(),
            )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
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

    fun onDelete(product: ProductEntity) {
        viewModelScope.launch { repository.deleteProduct(product.id) }
    }
}
