package com.rangele.inventory.ui.update

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lielu.githubupdater.UpdateState

/** Fenêtre proposant une mise à jour trouvée au lancement ; ne s'affiche que si une version plus récente existe. */
@Composable
fun UpdatePrompt(viewModel: AppUpdateViewModel) {
    val state by viewModel.state.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()
    if (dismissed) return

    when (val s = state) {
        is UpdateState.UpdateAvailable ->
            AlertDialog(
                onDismissRequest = viewModel::onDismiss,
                title = { Text("Mise à jour disponible") },
                text = { Text("La version ${s.update.versionName} de Yakwa est disponible.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.onInstall(s.update) }) { Text("Installer") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismiss) { Text("Plus tard") }
                },
            )
        is UpdateState.Downloading ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Téléchargement…") },
                text = {
                    LinearProgressIndicator(
                        progress = { (s.progress.percentage ?: 0) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {},
            )
        else -> Unit
    }
}
