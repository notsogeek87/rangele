package com.rangele.inventory.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.PantryRepository
import com.rangele.inventory.ocr.ParsedReceiptLine
import com.rangele.inventory.ocr.ReceiptParser
import com.rangele.inventory.ocr.ReceiptTextRecognizer
import com.rangele.inventory.util.toEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScanUiState(
    val isProcessing: Boolean = false,
    val capturedImageUri: Uri? = null,
    val lines: List<ParsedReceiptLine> = emptyList(),
    val hasResults: Boolean = false,
    val isImported: Boolean = false,
    val error: String? = null,
    val availablePantries: List<PantryEntity> = emptyList(),
) {
    val includedCount: Int get() = lines.count { it.included }
}

class ScanViewModel(
    private val repository: InventoryRepository,
    private val textRecognizer: ReceiptTextRecognizer,
    private val receiptParser: ReceiptParser,
    private val pantryRepository: PantryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pantryRepository.observePantries().collect { pantries ->
                _uiState.update { it.copy(availablePantries = pantries) }
            }
        }
    }

    fun onPhotoCaptured(
        context: Context,
        uri: Uri,
    ) {
        _uiState.update { it.copy(capturedImageUri = uri, isProcessing = true, error = null) }
        viewModelScope.launch {
            val defaultPantryId =
                _uiState.value.availablePantries
                    .firstOrNull { it.isDefault }
                    ?.id
            runCatching {
                val text = textRecognizer.recognizeText(context, uri)
                receiptParser.parse(text).map { line ->
                    val match = repository.findPotentialMatch(line.name)
                    val withPantry = line.copy(pantryId = defaultPantryId)
                    if (match != null) {
                        withPantry.copy(matchedProductId = match.id, matchedProductName = match.name)
                    } else {
                        withPantry
                    }
                }
            }.onSuccess { lines ->
                _uiState.update { it.copy(lines = lines, isProcessing = false, hasResults = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Impossible de lire ce ticket. Réessayez avec une photo plus nette.",
                    )
                }
            }
        }
    }

    fun onLineNameChanged(
        lineId: String,
        name: String,
    ) {
        updateLine(lineId) { it.copy(name = name, matchedProductId = null, matchedProductName = null) }
    }

    fun onLineQuantityTextChanged(
        lineId: String,
        quantityText: String,
    ) {
        val quantity = quantityText.replace(',', '.').toDoubleOrNull() ?: return
        updateLine(lineId) { it.copy(quantity = quantity) }
    }

    fun onLineUnitChanged(
        lineId: String,
        unit: QuantityUnit,
    ) {
        updateLine(lineId) { it.copy(unit = unit) }
    }

    fun onLineIncludedChanged(
        lineId: String,
        included: Boolean,
    ) {
        updateLine(lineId) { it.copy(included = included) }
    }

    fun onLineMatchCleared(lineId: String) {
        updateLine(lineId) { it.copy(matchedProductId = null, matchedProductName = null) }
    }

    fun onLineExpirationDateChanged(
        lineId: String,
        date: LocalDate?,
    ) {
        updateLine(lineId) { it.copy(expirationDate = date) }
    }

    fun onLineOpenedChanged(
        lineId: String,
        opened: Boolean,
    ) {
        updateLine(lineId) { it.copy(opened = opened) }
    }

    fun onLinePantryChanged(
        lineId: String,
        pantryId: Long?,
    ) {
        updateLine(lineId) { it.copy(pantryId = pantryId) }
    }

    fun onLineRemoved(lineId: String) {
        _uiState.update { state -> state.copy(lines = state.lines.filterNot { it.id == lineId }) }
    }

    fun onValidateImport() {
        viewModelScope.launch {
            _uiState.value.lines.filter { it.included }.forEach { line ->
                val matchedId = line.matchedProductId
                if (matchedId != null) {
                    repository.incrementExisting(matchedId, line.quantity)
                } else if (line.name.isNotBlank()) {
                    repository.insertAsNew(
                        name = line.name,
                        quantity = line.quantity,
                        unit = line.unit,
                        expirationDate = line.expirationDate?.toEpochMillis(),
                        opened = line.opened,
                        pantryId = line.pantryId,
                    )
                }
            }
            _uiState.update { it.copy(isImported = true) }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }

    private fun updateLine(
        lineId: String,
        transform: (ParsedReceiptLine) -> ParsedReceiptLine,
    ) {
        _uiState.update { state ->
            state.copy(lines = state.lines.map { if (it.id == lineId) transform(it) else it })
        }
    }
}
