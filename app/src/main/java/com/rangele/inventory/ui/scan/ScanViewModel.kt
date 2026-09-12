package com.rangele.inventory.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.ocr.ParsedReceiptLine
import com.rangele.inventory.ocr.ReceiptParser
import com.rangele.inventory.ocr.ReceiptTextRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val isProcessing: Boolean = false,
    val capturedImageUri: Uri? = null,
    val lines: List<ParsedReceiptLine> = emptyList(),
    val hasResults: Boolean = false,
    val isImported: Boolean = false,
    val error: String? = null,
) {
    val includedCount: Int get() = lines.count { it.included }
}

class ScanViewModel(
    private val repository: InventoryRepository,
    private val textRecognizer: ReceiptTextRecognizer,
    private val receiptParser: ReceiptParser,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(
        context: Context,
        uri: Uri,
    ) {
        _uiState.update { it.copy(capturedImageUri = uri, isProcessing = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val text = textRecognizer.recognizeText(context, uri)
                receiptParser.parse(text).map { line ->
                    val match = repository.findPotentialMatch(line.name)
                    if (match != null) {
                        line.copy(matchedProductId = match.id, matchedProductName = match.name)
                    } else {
                        line
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
                    repository.insertAsNew(line.name, line.quantity, line.unit)
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
