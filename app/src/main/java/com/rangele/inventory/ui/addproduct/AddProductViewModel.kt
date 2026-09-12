package com.rangele.inventory.ui.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProductUiState(
    val name: String = "",
    val quantityText: String = "1",
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val mergeSuggestion: ProductEntity? = null,
    val isSaved: Boolean = false,
) {
    val enteredQuantity: Double? get() = quantityText.replace(',', '.').toDoubleOrNull()
    val canSave: Boolean get() = name.isNotBlank() && (enteredQuantity?.let { it > 0 } == true)
}

class AddProductViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onQuantityTextChanged(text: String) {
        _uiState.update { it.copy(quantityText = text) }
    }

    fun onUnitChanged(unit: QuantityUnit) {
        _uiState.update { it.copy(unit = unit) }
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
                repository.insertAsNew(state.name.trim(), quantity, state.unit)
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
            repository.insertAsNew(state.name.trim(), quantity, state.unit)
            _uiState.update { it.copy(isSaved = true, mergeSuggestion = null) }
        }
    }

    fun onDismissMergeSuggestion() {
        _uiState.update { it.copy(mergeSuggestion = null) }
    }
}
