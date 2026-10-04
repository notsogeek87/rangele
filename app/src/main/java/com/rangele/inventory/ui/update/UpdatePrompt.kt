package com.rangele.inventory.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.lielu.githubupdater.UpdateError
import com.lielu.githubupdater.UpdateState

/** Fenêtre proposant une mise à jour trouvée au lancement, avec les étapes à suivre expliquées. */
@Composable
fun UpdatePrompt(viewModel: AppUpdateViewModel) {
    val state by viewModel.state.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()
    val userStarted by viewModel.userStarted.collectAsState()

    // ON_START : au lancement ET quand l'app, restée en mémoire, repasse au premier plan.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.checkOnOpen() }

    if (dismissed) return

    when (val s = state) {
        is UpdateState.UpdateAvailable ->
            AlertDialog(
                onDismissRequest = viewModel::onDismiss,
                title = { Text("Mise à jour disponible") },
                text = {
                    UpdateSteps(
                        intro = "La version ${s.update.versionName} de Yakwa est prête.",
                        steps =
                            listOf(
                                "Touchez « Installer » : la mise à jour se télécharge.",
                                "Si Android demande d'autoriser Yakwa à installer des applications, " +
                                    "activez l'option, puis revenez en arrière.",
                                "Confirmez avec « Mettre à jour » quand Android le propose.",
                            ),
                    )
                },
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
        is UpdateState.Downloaded ->
            AlertDialog(
                onDismissRequest = viewModel::onDismiss,
                title = { Text("Mise à jour téléchargée") },
                text = {
                    if (viewModel.canInstallPackages()) {
                        UpdateSteps(
                            intro = "Il ne reste qu'à l'installer.",
                            steps = listOf("Touchez « Installer », puis confirmez avec « Mettre à jour »."),
                        )
                    } else {
                        UpdateSteps(
                            intro = "Android doit d'abord autoriser Yakwa à installer des applications.",
                            steps =
                                listOf(
                                    "Touchez « Installer » : les réglages Android s'ouvrent.",
                                    "Activez « Autoriser depuis cette source », puis revenez en arrière.",
                                    "Touchez de nouveau « Installer » et confirmez.",
                                ),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onInstallDownloaded(s.file) }) { Text("Installer") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismiss) { Text("Plus tard") }
                },
            )
        is UpdateState.Error ->
            if (userStarted) {
                AlertDialog(
                    onDismissRequest = viewModel::onDismiss,
                    title = { Text("Mise à jour impossible") },
                    text = { Text(updateErrorMessage(s.error)) },
                    confirmButton = {
                        TextButton(onClick = viewModel::onDismiss) { Text("OK") }
                    },
                )
            }
        else -> Unit
    }
}

@Composable
private fun UpdateSteps(
    intro: String,
    steps: List<String>,
) {
    Column {
        Text(intro, style = MaterialTheme.typography.bodyMedium)
        steps.forEachIndexed { index, step ->
            Text(
                "${index + 1}. $step",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "Vos données sont conservées.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

internal fun updateErrorMessage(error: UpdateError): String =
    when (error) {
        is UpdateError.NetworkError -> "Réseau indisponible. Réessayez plus tard."
        is UpdateError.RateLimit -> "Trop de requêtes vers GitHub. Réessayez plus tard."
        UpdateError.ReleaseNotFound -> "Aucune version publiée trouvée."
        is UpdateError.ApkNotFound -> "Aucun APK compatible dans la dernière version."
        UpdateError.InstallationNotAllowed -> "Autorisez l'installation d'applications inconnues pour Yakwa."
        else -> "La mise à jour a échoué : ${error.message}"
    }
