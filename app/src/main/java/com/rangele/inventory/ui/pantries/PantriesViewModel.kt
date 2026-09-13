package com.rangele.inventory.ui.pantries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.repository.PantryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PantriesUiState(
    val pantries: List<PantryEntity> = emptyList(),
)

class PantriesViewModel(
    private val repository: PantryRepository,
) : ViewModel() {
    val uiState: StateFlow<PantriesUiState> =
        repository
            .observePantries()
            .map { PantriesUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PantriesUiState())

    fun onCreatePantry(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createPantry(name) }
    }

    fun onRenamePantry(
        pantry: PantryEntity,
        newName: String,
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renamePantry(pantry, newName) }
    }

    fun onDeletePantry(pantry: PantryEntity) {
        viewModelScope.launch { repository.deletePantry(pantry) }
    }

    fun onSetDefaultPantry(pantryId: Long?) {
        viewModelScope.launch { repository.setDefaultPantry(pantryId) }
    }
}
