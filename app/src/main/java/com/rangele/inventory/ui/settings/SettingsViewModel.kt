package com.rangele.inventory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.local.entity.PantryEntity
import com.rangele.inventory.data.repository.PantryRepository
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.work.ExpirationCheckScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val delayDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
    val pantries: List<PantryEntity> = emptyList(),
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val pantryRepository: PantryRepository,
    private val scheduler: ExpirationCheckScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, pantryRepository.observePantries()) { settings, pantries ->
            SettingsUiState(settings.enabled, settings.delayDays, settings.hour, settings.minute, pantries)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Called once the caller confirmed (or didn't need) the POST_NOTIFICATIONS permission. */
    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            scheduler.apply(settingsRepository.settings.first())
        }
    }

    fun onDelayDaysChanged(days: Int) {
        if (days < 0) return
        viewModelScope.launch {
            settingsRepository.setDelayDays(days)
            scheduler.apply(settingsRepository.settings.first())
        }
    }

    fun onTimeChanged(
        hour: Int,
        minute: Int,
    ) {
        viewModelScope.launch {
            settingsRepository.setNotificationTime(hour, minute)
            scheduler.apply(settingsRepository.settings.first())
        }
    }

    fun onCreatePantry(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { pantryRepository.createPantry(name) }
    }

    fun onRenamePantry(
        pantry: PantryEntity,
        newName: String,
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch { pantryRepository.renamePantry(pantry, newName) }
    }

    fun onDeletePantry(pantry: PantryEntity) {
        viewModelScope.launch { pantryRepository.deletePantry(pantry) }
    }

    fun onSetDefaultPantry(pantryId: Long?) {
        viewModelScope.launch { pantryRepository.setDefaultPantry(pantryId) }
    }
}
