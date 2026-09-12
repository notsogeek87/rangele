package com.rangele.inventory.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import com.rangele.inventory.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val entries: List<HistoryEntryEntity> = emptyList(),
)

class HistoryViewModel(
    repository: HistoryRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        repository
            .observeEntries()
            .map { HistoryUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
