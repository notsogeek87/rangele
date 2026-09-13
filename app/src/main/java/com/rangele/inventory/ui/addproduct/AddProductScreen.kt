package com.rangele.inventory.ui.addproduct

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rangele.inventory.ui.components.CategoryDropdown
import com.rangele.inventory.ui.components.ExpirationDateField
import com.rangele.inventory.ui.components.OpenedCheckbox
import com.rangele.inventory.ui.components.PantryDropdown
import com.rangele.inventory.ui.components.UnitDropdown
import com.rangele.inventory.ui.theme.ShapeSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un produit") },
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
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Nom du produit") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.quantityText,
                onValueChange = viewModel::onQuantityTextChanged,
                label = { Text("Quantité") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            UnitDropdown(
                selectedUnit = uiState.unit,
                onUnitSelected = viewModel::onUnitChanged,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            ExpirationDateField(
                date = uiState.expirationDate,
                onDateChanged = viewModel::onExpirationDateChanged,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            OpenedCheckbox(
                opened = uiState.opened,
                onOpenedChanged = viewModel::onOpenedChanged,
                modifier = Modifier.padding(top = 4.dp),
            )

            CategoryDropdown(
                categories = uiState.availableCategories,
                selectedCategory = uiState.category,
                onCategorySelected = viewModel::onCategoryChanged,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            PantryDropdown(
                pantries = uiState.availablePantries,
                selectedPantryId = uiState.pantryId,
                onPantrySelected = viewModel::onPantryChanged,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            OutlinedTextField(
                value = uiState.lowStockThresholdText,
                onValueChange = viewModel::onLowStockThresholdTextChanged,
                label = { Text("Seuil de stock bas (optionnel)") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            Button(
                onClick = viewModel::onSaveClicked,
                enabled = uiState.canSave,
                shape = ShapeSmall,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Text("Ajouter à l'inventaire")
            }
        }
    }

    uiState.mergeSuggestion?.let { existing ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissMergeSuggestion,
            title = { Text("Produit similaire trouvé") },
            text = {
                Text(
                    "« ${existing.name} » est déjà dans votre inventaire " +
                        "(${formatPlain(existing.quantity)} ${existing.quantityUnit.label}). " +
                        "Voulez-vous ajouter ou retirer la quantité saisie ?",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmMergeIntoExisting) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRemoveFromExisting) {
                    Text("Retirer")
                }
            },
        )
    }
}

private fun formatPlain(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
