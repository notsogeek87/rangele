package com.rangele.inventory.ui.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.CategoryRepository
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.util.toEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddProductUiState(
    val name: String = "",
    val quantityText: String = "1",
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val expirationDate: LocalDate? = null,
    val opened: Boolean = false,
    val category: String? = null,
    val availableCategories: List<String> = emptyList(),
    val lowStockThresholdText: String = "",
    val mergeSuggestion: ProductEntity? = null,
    val isSaved: Boolean = false,
) {
    val enteredQuantity: Double? get() = quantityText.replace(',', '.').toDoubleOrNull()
    val enteredLowStockThreshold: Double? get() = lowStockThresholdText.replace(',', '.').toDoubleOrNull()
    val canSave: Boolean get() = name.isNotBlank() && (enteredQuantity?.let { it > 0 } == true)
}

class AddProductViewModel(
    private val repository: InventoryRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(availableCategories = categories.map { category -> category.name }) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onQuantityTextChanged(text: String) {
        _uiState.update { it.copy(quantityText = text) }
    }

    fun onUnitChanged(unit: QuantityUnit) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun onExpirationDateChanged(date: LocalDate?) {
        _uiState.update { it.copy(expirationDate = date) }
    }

    fun onOpenedChanged(opened: Boolean) {
        _uiState.update { it.copy(opened = opened) }
    }

    fun onCategoryChanged(category: String?) {
        _uiState.update { it.copy(category = category) }
    }

    fun onLowStockThresholdTextChanged(text: String) {
        _uiState.update { it.copy(lowStockThresholdText = text) }
    }

    fun onSaveClicked() {
        val state = _uiState.value
        val quantity = state.enteredQuantity ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            val match = repository.findPotentialMatch(state.name)
            if (match != null) {
                _uiState.update { it.copy(mergeSuggestion = match) }
            } else {
                repository.insertAsNew(
                    name = state.name.trim(),
                    quantity = quantity,
                    unit = state.unit,
                    expirationDate = state.expirationDate?.toEpochMillis(),
                    category = state.category,
                    lowStockThreshold = state.enteredLowStockThreshold,
                    opened = state.opened,
                )
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun onConfirmMergeIntoExisting() {
        val state = _uiState.value
        val match = state.mergeSuggestion ?: return
        val quantity = state.enteredQuantity ?: return
        viewModelScope.launch {
            repository.incrementExisting(match.id, quantity)
            _uiState.update { it.copy(isSaved = true, mergeSuggestion = null) }
        }
    }

    fun onCreateSeparateProduct() {
        val state = _uiState.value
        val quantity = state.enteredQuantity ?: return
        viewModelScope.launch {
            repository.insertAsNew(
                name = state.name.trim(),
                quantity = quantity,
                unit = state.unit,
                expirationDate = state.expirationDate?.toEpochMillis(),
                category = state.category,
                lowStockThreshold = state.enteredLowStockThreshold,
                opened = state.opened,
            )
            _uiState.update { it.copy(isSaved = true, mergeSuggestion = null) }
        }
    }

    fun onDismissMergeSuggestion() {
        _uiState.update { it.copy(mergeSuggestion = null) }
    }
}
