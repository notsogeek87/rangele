package com.rangele.inventory.ui.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.ocr.ParsedReceiptLine
import com.rangele.inventory.ui.components.ExpirationDateField
import com.rangele.inventory.ui.components.UnitDropdown
import com.rangele.inventory.ui.theme.ShapeSmall
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    viewModel: ScanViewModel,
    onBackClick: () -> Unit,
    onImported: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isImported) {
        if (uiState.isImported) onImported()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vérifier le ticket") },
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
        bottomBar = {
            Button(
                onClick = viewModel::onValidateImport,
                enabled = uiState.includedCount > 0,
                shape = ShapeSmall,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Valider (${uiState.includedCount} article${if (uiState.includedCount > 1) "s" else ""})")
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isProcessing ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Lecture du ticket en cours…",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                uiState.error != null ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(uiState.error.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }

                uiState.lines.isEmpty() ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Aucun article détecté sur ce ticket. Réessayez avec une photo plus nette.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.lines, key = { it.id }) { line ->
                            ReceiptLineRow(
                                line = line,
                                onNameChanged = { viewModel.onLineNameChanged(line.id, it) },
                                onQuantityTextChanged = { viewModel.onLineQuantityTextChanged(line.id, it) },
                                onUnitChanged = { viewModel.onLineUnitChanged(line.id, it) },
                                onIncludedChanged = { viewModel.onLineIncludedChanged(line.id, it) },
                                onMatchCleared = { viewModel.onLineMatchCleared(line.id) },
                                onRemove = { viewModel.onLineRemoved(line.id) },
                                onExpirationDateChanged = { viewModel.onLineExpirationDateChanged(line.id, it) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ReceiptLineRow(
    line: ParsedReceiptLine,
    onNameChanged: (String) -> Unit,
    onQuantityTextChanged: (String) -> Unit,
    onUnitChanged: (QuantityUnit) -> Unit,
    onIncludedChanged: (Boolean) -> Unit,
    onMatchCleared: () -> Unit,
    onRemove: () -> Unit,
    onExpirationDateChanged: (LocalDate?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = line.included, onCheckedChange = onIncludedChanged)

                OutlinedTextField(
                    value = line.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Produit") },
                )

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Retirer cette ligne")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = formatPlainQuantity(line.quantity),
                    onValueChange = onQuantityTextChanged,
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    label = { Text("Qté") },
                )

                UnitDropdown(
                    selectedUnit = line.unit,
                    onUnitSelected = onUnitChanged,
                    modifier = Modifier.width(140.dp).padding(start = 8.dp),
                )
            }

            ExpirationDateField(
                date = line.expirationDate,
                onDateChanged = onExpirationDateChanged,
                label = "Péremption (optionnel)",
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp),
            )

            if (line.matchedProductName != null) {
                AssistChip(
                    onClick = onMatchCleared,
                    label = { Text("Fusion avec « ${line.matchedProductName} »") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Créer un nouveau produit à la place",
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(),
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                )
            }
        }
    }
}

private fun formatPlainQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
