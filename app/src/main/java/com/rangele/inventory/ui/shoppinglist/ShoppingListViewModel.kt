package com.rangele.inventory.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ShoppingListUiState(
    val products: List<ProductEntity> = emptyList(),
    val checkedIds: Set<Long> = emptySet(),
) {
    val checkedProducts: List<ProductEntity> get() = products.filter { it.id in checkedIds }
}

class ShoppingListViewModel(
    repository: InventoryRepository,
) : ViewModel() {
    private val checkedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ShoppingListUiState> =
        combine(repository.observeLowStockProducts(), checkedIds) { products, checked ->
            ShoppingListUiState(products, checked.intersect(products.map { it.id }.toSet()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingListUiState())

    fun onToggle(product: ProductEntity) {
        checkedIds.update { if (product.id in it) it - product.id else it + product.id }
    }

    fun buildShareText(): String = uiState.value.checkedProducts.joinToString(separator = "\n") { "- ${it.name}" }
}
