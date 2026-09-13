package com.rangele.inventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.repository.CategoryRepository
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.ItemDetails
import com.rangele.inventory.data.repository.PantryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val pantryRepository: PantryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val sortMode = MutableStateFlow(SortMode.NAME)
    private val selectedCategory = MutableStateFlow<String?>(null)

    private var pantryPromptDismissed = false
    private val _showCreatePantryPrompt = MutableStateFlow(false)

    /** True once pantries have loaded and none exist yet, until the user creates one or dismisses the prompt. */
    val showCreatePantryPrompt: StateFlow<Boolean> = _showCreatePantryPrompt.asStateFlow()

    private val _editingItems = MutableStateFlow<List<ItemDetails>?>(null)

    /** Items of the product currently open in the edit dialog, null while loading. */
    val editingItems: StateFlow<List<ItemDetails>?> = _editingItems.asStateFlow()

    init {
        viewModelScope.launch {
            pantryRepository.observePantries().collect { pantries ->
                if (!pantryPromptDismissed) {
                    _showCreatePantryPrompt.value = pantries.isEmpty()
                }
            }
        }
    }

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

    /**
     * Saves every field of a continuous-unit product's sheet.
     *
     * Les trois appels sont séquentiels dans une seule coroutine, et ce n'est pas un détail :
     * chacun relit le produit puis réécrit la ligne entière (Room `@Update`). Lancés en
     * parallèle, ils partent tous du même état d'avant modification et le dernier à écrire
     * écrase les deux autres — le seuil saisi disparaissait silencieusement.
     */
    fun onProductSheetSaved(
        product: ProductEntity,
        quantity: Double,
        expirationDate: Long?,
        opened: Boolean,
        lowStockThreshold: Double?,
    ) {
        viewModelScope.launch {
            repository.setQuantity(product.id, quantity)
            repository.updateDetails(product.id, expirationDate, opened)
            repository.updateLowStockThreshold(product.id, lowStockThreshold)
        }
    }

    /** Loads the items of [productId] for the edit dialog to show. */
    fun onEditDialogOpened(productId: Long) {
        viewModelScope.launch { _editingItems.value = repository.getItems(productId) }
    }

    fun onEditDialogClosed() {
        _editingItems.value = null
    }

    /** Même sérialisation que [onProductSheetSaved], pour un produit en unité discrète. */
    fun onItemsSaved(
        product: ProductEntity,
        items: List<ItemDetails>,
        lowStockThreshold: Double?,
    ) {
        viewModelScope.launch {
            repository.saveItems(product.id, items)
            repository.updateLowStockThreshold(product.id, lowStockThreshold)
        }
    }

    /** Bascule l'ajout manuel à la liste de courses, complément du seuil pour les produits encore en stock. */
    fun onToggleShoppingList(product: ProductEntity) {
        viewModelScope.launch { repository.setInShoppingList(product.id, !product.inShoppingList) }
    }

    fun onDelete(product: ProductEntity) {
        viewModelScope.launch { repository.deleteProduct(product.id) }
    }

    /** Creates the user's first pantry and makes it the default, since it's the only one so far. */
    fun onCreateFirstPantry(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = pantryRepository.createPantry(name)
            pantryRepository.setDefaultPantry(id)
            pantryPromptDismissed = true
            _showCreatePantryPrompt.value = false
        }
    }

    fun onDismissCreatePantryPrompt() {
        pantryPromptDismissed = true
        _showCreatePantryPrompt.value = false
    }
}
