package com.rangele.inventory.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.barcode.OffLookupResult
import com.rangele.inventory.barcode.OffProduct
import com.rangele.inventory.barcode.OffProductRepository
import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.CategoryRepository
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.PantryRepository
import com.rangele.inventory.util.toEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Where the barcode flow currently stands, from the camera preview through to a resolved lookup. */
sealed interface BarcodeLookupState {
    data object Scanning : BarcodeLookupState

    data object Loading : BarcodeLookupState

    data class Found(
        val product: OffProduct,
    ) : BarcodeLookupState

    /** The scanned barcode already matches a product in the inventory: offer to bump its quantity instead. */
    data class AlreadyInInventory(
        val existing: ProductEntity,
    ) : BarcodeLookupState

    data class NotFound(
        val barcode: String,
    ) : BarcodeLookupState

    data class Error(
        val barcode: String,
    ) : BarcodeLookupState
}

data class BarcodeUiState(
    val lookup: BarcodeLookupState = BarcodeLookupState.Scanning,
    val name: String = "",
    val quantityText: String = "1",
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val expirationDate: LocalDate? = null,
    val category: String? = null,
    val nutriscore: String? = null,
    val availableCategories: List<String> = emptyList(),
    val pantryId: Long? = null,
    val availablePantries: List<PantryEntity> = emptyList(),
    val isSaved: Boolean = false,
) {
    val enteredQuantity: Double? get() = quantityText.replace(',', '.').toDoubleOrNull()
    val canSave: Boolean get() = name.isNotBlank() && (enteredQuantity?.let { it > 0 } == true)
}

class BarcodeScanViewModel(
    private val inventoryRepository: InventoryRepository,
    private val categoryRepository: CategoryRepository,
    private val pantryRepository: PantryRepository,
    private val offProductRepository: OffProductRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BarcodeUiState())
    val uiState: StateFlow<BarcodeUiState> = _uiState.asStateFlow()

    private var pendingBarcode: String? = null
    private var pantryManuallySelected = false

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(availableCategories = categories.map { category -> category.name }) }
            }
        }
        viewModelScope.launch {
            pantryRepository.observePantries().collect { pantries ->
                val defaultPantryId =
                    pantries
                        .firstOrNull { it.isDefault }
                        ?.id
                _uiState.update { state ->
                    val pantryId = if (pantryManuallySelected) state.pantryId else defaultPantryId
                    state.copy(availablePantries = pantries, pantryId = pantryId)
                }
            }
        }
    }

    /** Called for every detected frame; ignored once a scan is already being resolved or shown. */
    fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.lookup !is BarcodeLookupState.Scanning) return
        pendingBarcode = barcode
        _uiState.update { it.copy(lookup = BarcodeLookupState.Loading) }
        viewModelScope.launch {
            val existing = inventoryRepository.findByBarcode(barcode)
            if (existing != null) {
                _uiState.update { it.copy(lookup = BarcodeLookupState.AlreadyInInventory(existing)) }
                return@launch
            }
            when (val result = offProductRepository.lookupProduct(barcode)) {
                is OffLookupResult.Found -> {
                    val state = _uiState.value
                    _uiState.update {
                        it.copy(
                            lookup = BarcodeLookupState.Found(result.product),
                            name = result.product.name,
                            category = result.product.category?.takeIf { c -> c in state.availableCategories },
                            nutriscore = result.product.nutriscore,
                        )
                    }
                }
                is OffLookupResult.NotFound ->
                    _uiState.update {
                        it.copy(lookup = BarcodeLookupState.NotFound(barcode), name = "", nutriscore = null)
                    }
                OffLookupResult.NetworkError ->
                    _uiState.update { it.copy(lookup = BarcodeLookupState.Error(barcode)) }
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

    fun onCategoryChanged(category: String?) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPantryChanged(pantryId: Long?) {
        pantryManuallySelected = true
        _uiState.update { it.copy(pantryId = pantryId) }
    }

    /** Confirms adding the found (or manually-filled, when not found) product to the inventory. */
    fun onSaveClicked() {
        val state = _uiState.value
        val quantity = state.enteredQuantity ?: return
        val barcode = pendingBarcode ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            inventoryRepository.insertAsNew(
                name = state.name.trim(),
                quantity = quantity,
                unit = state.unit,
                expirationDate = state.expirationDate?.toEpochMillis(),
                category = state.category,
                barcode = barcode,
                pantryId = state.pantryId,
                nutriscore = state.nutriscore,
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    /**
     * Ajouter/Retirer on the "already present" screen: one step of the product's own unit, then
     * back to the list. Adding tags the newly added unit(s) with the entered expiration date, if any.
     */
    fun onAdjustExistingQuantity(delta: Double) {
        val existing = (_uiState.value.lookup as? BarcodeLookupState.AlreadyInInventory)?.existing ?: return
        val expirationDate = _uiState.value.expirationDate?.toEpochMillis()
        viewModelScope.launch {
            if (delta > 0) {
                inventoryRepository.incrementExisting(existing.id, delta, expirationDate)
            } else {
                inventoryRepository.adjustQuantity(existing.id, delta)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    /** Re-runs the Open Food Facts lookup for the barcode already scanned, after a network error. */
    fun onRetryLookup() {
        val barcode = pendingBarcode ?: return
        _uiState.update { it.copy(lookup = BarcodeLookupState.Scanning) }
        onBarcodeDetected(barcode)
    }

    /**
     * Force une actualisation depuis Open Food Facts en ignorant le cache — pour un futur bouton
     * « Actualiser les informations » sur un produit déjà trouvé.
     */
    fun onForceRefresh() {
        val barcode = pendingBarcode ?: return
        _uiState.update { it.copy(lookup = BarcodeLookupState.Loading) }
        viewModelScope.launch {
            when (val result = offProductRepository.refreshProduct(barcode)) {
                is OffLookupResult.Found ->
                    _uiState.update { it.copy(lookup = BarcodeLookupState.Found(result.product)) }
                is OffLookupResult.NotFound ->
                    _uiState.update { it.copy(lookup = BarcodeLookupState.NotFound(barcode)) }
                OffLookupResult.NetworkError ->
                    _uiState.update { it.copy(lookup = BarcodeLookupState.Error(barcode)) }
            }
        }
    }

    fun onRetryScan() {
        pendingBarcode = null
        pantryManuallySelected = false
        _uiState.update { state ->
            val defaultPantryId =
                state.availablePantries
                    .firstOrNull { it.isDefault }
                    ?.id
            BarcodeUiState(
                lookup = BarcodeLookupState.Scanning,
                availableCategories = state.availableCategories,
                availablePantries = state.availablePantries,
                pantryId = defaultPantryId,
            )
        }
    }
}
