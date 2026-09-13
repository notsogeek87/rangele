package com.rangele.inventory.ui.pantries

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rangele.inventory.data.local.entity.PantryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantriesScreen(
    viewModel: PantriesViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var pantryPendingRename by remember { mutableStateOf<PantryEntity?>(null) }
    var pantryPendingDelete by remember { mutableStateOf<PantryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Placards") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Créer un placard")
            }
        },
    ) { paddingValues ->
        if (uiState.pantries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Aucun placard pour le moment.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Text(
                    "Sélectionnez le placard présélectionné à l'ajout d'un produit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.pantries, key = { it.id }) { pantry ->
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
        }
    }

    if (showCreateDialog) {
        PantryNameDialog(
            title = "Nouveau placard",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.onCreatePantry(name)
                showCreateDialog = false
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
