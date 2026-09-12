package com.rangele.inventory.ui.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rangele.inventory.R
import com.rangele.inventory.data.local.entity.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onAddProductClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var productPendingEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productPendingDelete by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon inventaire") },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_yakwa_mark),
                        contentDescription = "Yakwa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp).height(24.dp),
                    )
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = onScanReceiptClick,
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                    text = { Text("Scanner un ticket") },
                )
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(onClick = onAddProductClick) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un produit")
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Rechercher un produit") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            if (uiState.products.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            if (uiState.searchQuery.isBlank()) {
                                "Votre placard est vide. Ajoutez un produit ou scannez un ticket de caisse."
                            } else {
                                "Aucun produit ne correspond à « ${uiState.searchQuery} »."
                            },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            onIncrement = { viewModel.onIncrement(product) },
                            onDecrement = { viewModel.onDecrement(product) },
                            onQuantityClick = { productPendingEdit = product },
                            onDeleteClick = { productPendingDelete = product },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    productPendingEdit?.let { product ->
        EditQuantityDialog(
            product = product,
            onDismiss = { productPendingEdit = null },
            onConfirm = { newQuantity ->
                viewModel.onQuantitySet(product, newQuantity)
                productPendingEdit = null
            },
        )
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Supprimer ${product.name} ?") },
            text = { Text("Ce produit sera retiré de votre inventaire.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDelete(product)
                    productPendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun ProductRow(
    product: ProductEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuer la quantité")
        }

        TextButton(onClick = onQuantityClick) {
            Text(formatQuantity(product.quantity, product.quantityUnit.label))
        }

        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = "Augmenter la quantité")
        }

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuantityDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember(product.id) { mutableStateOf(formatPlainQuantity(product.quantity)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quantité de ${product.name}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                suffix = { Text(product.quantityUnit.label) },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.replace(',', '.').toDoubleOrNull()?.let(onConfirm)
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private fun formatPlainQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()

private fun formatQuantity(
    quantity: Double,
    unitLabel: String,
): String = "${formatPlainQuantity(quantity)} $unitLabel"
