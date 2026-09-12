package com.rangele.inventory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.work.ExpirationCheckScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val delayDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduler: ExpirationCheckScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        settingsRepository.settings
            .map { SettingsUiState(it.enabled, it.delayDays, it.hour, it.minute) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

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
}
