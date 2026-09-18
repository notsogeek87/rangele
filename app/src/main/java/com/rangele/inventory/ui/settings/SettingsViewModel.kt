package com.rangele.inventory.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rangele.inventory.backup.BackupRepository
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.data.settings.ThemeMode
import com.rangele.inventory.work.ExpirationCheckScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val delayDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
    val lastBackupAt: Long? = null,
    val backupInProgress: Boolean = false,
    val backupMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

private data class BackupOpState(
    val inProgress: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduler: ExpirationCheckScheduler,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val backupState = MutableStateFlow(BackupOpState())

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.settings,
            settingsRepository.lastBackupTimestamp,
            backupState,
            settingsRepository.themeMode,
        ) { settings, lastBackupAt, backupOp, themeMode ->
            SettingsUiState(
                notificationsEnabled = settings.enabled,
                delayDays = settings.delayDays,
                hour = settings.hour,
                minute = settings.minute,
                lastBackupAt = lastBackupAt,
                backupInProgress = backupOp.inProgress,
                backupMessage = backupOp.message,
                themeMode = themeMode,
            )
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

    /** [uri] comes from the ACTION_CREATE_DOCUMENT picker, so it may point at Google Drive, Files, etc. */
    fun onExportRequested(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        viewModelScope.launch {
            backupState.value = BackupOpState(inProgress = true)
            val message =
                try {
                    val stream =
                        contentResolver.openOutputStream(uri)
                            ?: error("Impossible d'ouvrir le fichier de destination")
                    stream.use { backupRepository.exportTo(it) }
                    settingsRepository.setLastBackupTimestamp(System.currentTimeMillis())
                    "Sauvegarde enregistrée."
                } catch (e: Exception) {
                    "Échec de la sauvegarde : ${e.message}"
                }
            backupState.value = BackupOpState(message = message)
        }
    }

    fun onImportRequested(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        viewModelScope.launch {
            backupState.value = BackupOpState(inProgress = true)
            val message =
                try {
                    val stream =
                        contentResolver.openInputStream(uri)
                            ?: error("Impossible de lire le fichier sélectionné")
                    val result = stream.use { backupRepository.importFrom(it) }
                    "Sauvegarde restaurée : ${result.products} produit(s), " +
                        "${result.categories} catégorie(s), ${result.historyEntries} entrée(s) d'historique."
                } catch (e: Exception) {
                    "Échec de la restauration : ${e.message}"
                }
            backupState.value = BackupOpState(message = message)
        }
    }

    fun onBackupMessageShown() {
        backupState.update { it.copy(message = null) }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
}
