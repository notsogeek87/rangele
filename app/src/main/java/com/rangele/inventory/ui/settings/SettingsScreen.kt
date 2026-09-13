package com.rangele.inventory.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rangele.inventory.data.local.entity.PantryEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTimeDialog by remember { mutableStateOf(false) }
    var showCreatePantryDialog by remember { mutableStateOf(false) }
    var pantryPendingRename by remember { mutableStateOf<PantryEntity?>(null) }
    var pantryPendingDelete by remember { mutableStateOf<PantryEntity?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.onNotificationsToggled(true)
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) viewModel.onExportRequested(context.contentResolver, uri)
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.onImportRequested(context.contentResolver, uri)
        }

    LaunchedEffect(uiState.backupMessage) {
        val message = uiState.backupMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onBackupMessageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications de péremption", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Une vérification est faite une fois par jour.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { checked ->
                        if (checked && needsNotificationPermission(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onNotificationsToggled(checked)
                        }
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Alerte avant échéance",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { viewModel.onDelayDaysChanged(uiState.delayDays - 1) },
                    enabled = uiState.delayDays > 0,
                ) {
                    Text("−")
                }
                Text("${uiState.delayDays} jour${if (uiState.delayDays > 1) "s" else ""}")
                TextButton(onClick = { viewModel.onDelayDaysChanged(uiState.delayDays + 1) }) {
                    Text("+")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Heure de la vérification",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTimeDialog = true }) {
                    Text("%02d:%02d".format(uiState.hour, uiState.minute))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Placards",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showCreatePantryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Ajouter")
                }
            }

            if (uiState.pantries.isEmpty()) {
                Text(
                    "Aucun placard pour le moment.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    "Sélectionnez le placard préselectionné à l'ajout d'un produit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.pantries.forEach { pantry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = pantry.isDefault,
                                    onClick = {
                                        viewModel.onSetDefaultPantry(if (pantry.isDefault) null else pantry.id)
                                    },
                                )
                                Text(
                                    pantry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { pantryPendingRename = pantry }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Renommer")
                                }
                                IconButton(onClick = { pantryPendingDelete = pantry }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Sauvegarde", style = MaterialTheme.typography.titleMedium)
            Text(
                formatLastBackup(uiState.lastBackupAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { exportLauncher.launch(defaultBackupFileName()) },
                    enabled = !uiState.backupInProgress,
                ) {
                    Text("Sauvegarder maintenant")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { showRestoreConfirm = true },
                    enabled = !uiState.backupInProgress,
                ) {
                    Text("Restaurer…")
                }
            }
            if (uiState.backupInProgress) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp).size(24.dp))
            }
            Text(
                "Choisissez où enregistrer le fichier — Google Drive, Fichiers, etc.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (showTimeDialog) {
        NotificationTimeDialog(
            initialHour = uiState.hour,
            initialMinute = uiState.minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.onTimeChanged(hour, minute)
                showTimeDialog = false
            },
        )
    }

    if (showCreatePantryDialog) {
        PantryNameDialog(
            title = "Nouveau placard",
            initialName = "",
            onDismiss = { showCreatePantryDialog = false },
            onConfirm = { name ->
                viewModel.onCreatePantry(name)
                showCreatePantryDialog = false
            },
        )
    }

    pantryPendingRename?.let { pantry ->
        PantryNameDialog(
            title = "Renommer « ${pantry.name} »",
            initialName = pantry.name,
            onDismiss = { pantryPendingRename = null },
            onConfirm = { name ->
                viewModel.onRenamePantry(pantry, name)
                pantryPendingRename = null
            },
        )
    }

    pantryPendingDelete?.let { pantry ->
        AlertDialog(
            onDismissRequest = { pantryPendingDelete = null },
            title = { Text("Supprimer « ${pantry.name} » ?") },
            text = { Text("Les produits de ce placard resteront dans l'inventaire, sans placard.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeletePantry(pantry)
                    pantryPendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pantryPendingDelete = null }) { Text("Annuler") }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurer une sauvegarde") },
            text = {
                Text(
                    "Cela remplacera tout l'inventaire actuel (produits, catégories, historique) " +
                        "par le contenu du fichier choisi. Cette action est irréversible. Continuer ?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Restaurer") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Annuler") }
            },
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    return granted != PackageManager.PERMISSION_GRANTED
}

private fun defaultBackupFileName(): String {
    val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atZone(ZoneId.systemDefault()))
    return "rangele_sauvegarde_$date.json"
}

private fun formatLastBackup(timestamp: Long?): String {
    if (timestamp == null) return "Aucune sauvegarde effectuée."
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")
    val formatted = formatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
    return "Dernière sauvegarde : $formatted"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Heure de la vérification quotidienne") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nom") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
