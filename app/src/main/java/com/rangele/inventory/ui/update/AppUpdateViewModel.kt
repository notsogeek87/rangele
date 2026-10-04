package com.rangele.inventory.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lielu.githubupdater.UpdateInfo
import com.lielu.githubupdater.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Vérifie automatiquement les mises à jour à l'ouverture de l'app et pilote la fenêtre qui les propose.
 * L'état brut est celui de [UpdateManager], partagé avec l'écran Paramètres.
 */
class AppUpdateViewModel(
    private val updateManager: UpdateManager,
    updatesEnabled: Boolean,
) : ViewModel() {
    val state = updateManager.state

    private val _dismissed = MutableStateFlow(false)

    /** `true` une fois « Plus tard » touché : la fenêtre ne revient qu'au prochain lancement. */
    val dismissed: StateFlow<Boolean> = _dismissed

    init {
        if (updatesEnabled) {
            // Les erreurs (hors ligne, quota GitHub…) sont publiées dans `state` ; ici on reste silencieux.
            viewModelScope.launch { runCatching { updateManager.checkForUpdate() } }
        }
    }

    fun onDismiss() {
        _dismissed.value = true
    }

    fun onInstall(update: UpdateInfo) {
        viewModelScope.launch {
            runCatching {
                val apk = updateManager.downloadUpdate(update)
                if (updateManager.canInstallPackages()) {
                    updateManager.installUpdate(apk)
                } else {
                    updateManager.openInstallPermissionSettings()
                }
            }
        }
    }
}
