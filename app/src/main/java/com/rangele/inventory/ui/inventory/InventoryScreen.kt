package com.rangele.inventory.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rangele.inventory.R
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.ui.components.ExpirationDateField
import com.rangele.inventory.ui.components.OpenedCheckbox
import com.rangele.inventory.ui.theme.WarningOrange
import com.rangele.inventory.util.ExpirationStatus
import com.rangele.inventory.util.toEpochMillis
import com.rangele.inventory.util.toLocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onAddProductClick: () -> Unit,
    onScanBarcodeClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onImportReceiptClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onPantriesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShoppingListClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreatePantryPrompt by viewModel.showCreatePantryPrompt.collectAsState()
    var productPendingEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productPendingDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchQueryInput by remember { mutableStateOf(uiState.searchQuery) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                actions = {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Trier")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Trier par nom") },
                            onClick = {
                                viewModel.onSortModeChanged(SortMode.NAME)
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Trier par date de péremption") },
                            onClick = {
                                viewModel.onSortModeChanged(SortMode.EXPIRATION)
                                sortMenuExpanded = false
                            },
                        )
                    }

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Catégories") },
                            onClick = {
                                menuExpanded = false
                                onCategoriesClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Placards") },
                            onClick = {
                                menuExpanded = false
                                onPantriesClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Liste de courses suggérée") },
                            onClick = {
                                menuExpanded = false
                                onShoppingListClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Historique") },
                            onClick = {
                                menuExpanded = false
                                onHistoryClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            },
                        )
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
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = onScanBarcodeClick,
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scanner un code-barres") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = onScanReceiptClick,
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                    text = { Text("Scanner un ticket") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = onImportReceiptClick,
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    text = { Text("Importer un ticket") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = onAddProductClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un produit")
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQueryInput,
                onValueChange = {
                    searchQueryInput = it
                    viewModel.onSearchQueryChanged(it)
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Rechercher un produit") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            if (uiState.availableCategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { viewModel.onCategoryFilterChanged(null) },
                            label = { Text("Toutes") },
                        )
                    }
                    items(uiState.availableCategories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = {
                                viewModel.onCategoryFilterChanged(
                                    if (uiState.selectedCategory == category) null else category,
                                )
                            },
                            label = { Text(category) },
                        )
                    }
                }
            }

            if (uiState.products.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            if (searchQueryInput.isBlank()) {
                                "Votre placard est vide. Ajoutez un produit ou scannez un ticket de caisse."
                            } else {
                                "Aucun produit ne correspond à « $searchQueryInput »."
                            },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            onIncrement = { viewModel.onIncrement(product) },
                            onDecrement = { viewModel.onDecrement(product) },
                            onQuantityClick = { productPendingEdit = product },
                            onDeleteClick = { productPendingDelete = product },
                        )
                    }
                }
            }
        }
    }

    productPendingEdit?.let { product ->
        if (product.quantityUnit.step == 1.0) {
            val editingItems by viewModel.editingItems.collectAsState()
            LaunchedEffect(product.id) { viewModel.onEditDialogOpened(product.id) }
            editingItems?.let { items ->
                EditItemsDialog(
                    product = product,
                    items = items,
                    onDismiss = {
                        viewModel.onEditDialogClosed()
                        productPendingEdit = null
                    },
                    onConfirm = { expirationDates, opened ->
                        viewModel.onItemsSaved(product, expirationDates, opened)
                        viewModel.onEditDialogClosed()
                        productPendingEdit = null
                    },
                )
            }
        } else {
            EditQuantityDialog(
                product = product,
                onDismiss = { productPendingEdit = null },
                onConfirm = { newQuantity, expirationDate, opened ->
                    viewModel.onQuantitySet(product, newQuantity)
                    viewModel.onDetailsUpdated(product, expirationDate, opened)
                    productPendingEdit = null
                },
            )
        }
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

    if (showCreatePantryPrompt) {
        CreateFirstPantryDialog(
            onDismiss = viewModel::onDismissCreatePantryPrompt,
            onConfirm = viewModel::onCreateFirstPantry,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFirstPantryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer votre premier placard") },
        text = {
            Column {
                Text(
                    "Un placard permet de ranger vos produits (ex. Cuisine, Congélateur). " +
                        "Vous pourrez en créer d'autres plus tard depuis le menu « Placards ».",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Nom du placard") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Plus tard") }
        },
    )
}

@Composable
private fun ProductRow(
    product: ProductEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                product.expirationDate?.let { expirationDate ->
                    val status = ExpirationStatus.of(expirationDate.toLocalDate())
                    Text(
                        text = "Péremption : ${expirationDate.toLocalDate().format(DATE_FORMAT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = expirationColor(status),
                    )
                }
                if (product.opened) {
                    Text(
                        text = "Entamé",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningOrange,
                    )
                }
            }

            IconButton(
                onClick = onDecrement,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuer la quantité")
            }

            TextButton(onClick = onQuantityClick) {
                Text(formatQuantity(product.quantity, product.quantityUnit.label))
            }

            IconButton(
                onClick = onIncrement,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Augmenter la quantité")
            }

            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

@Composable
private fun expirationColor(status: ExpirationStatus): Color =
    when (status) {
        ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.error
        ExpirationStatus.SOON -> WarningOrange
        ExpirationStatus.OK, ExpirationStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/** Edits quantity, expiration date and opened status of a product in a continuous unit (weight/volume). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuantityDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, expirationDate: Long?, opened: Boolean) -> Unit,
) {
    var text by remember(product.id) { mutableStateOf(formatPlainQuantity(product.quantity)) }
    var expirationDate by remember(product.id) { mutableStateOf(product.expirationDate?.toLocalDate()) }
    var opened by remember(product.id) { mutableStateOf(product.opened) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${product.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    suffix = { Text(product.quantityUnit.label) },
                )
                ExpirationDateField(
                    date = expirationDate,
                    onDateChanged = { expirationDate = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OpenedCheckbox(
                    opened = opened,
                    onOpenedChanged = { opened = it },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.replace(',', '.').toDoubleOrNull()?.let { quantity ->
                    onConfirm(quantity, expirationDate?.toEpochMillis(), opened)
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/**
 * Edits a discrete-unit product (pièce/paquet): one row per unit in stock, each with its own
 * optional expiration date, instead of a single date shared by the whole quantity. Adding or
 * removing a row changes the quantity; the product must be deleted from the list to reach zero.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemsDialog(
    product: ProductEntity,
    items: List<Long?>,
    onDismiss: () -> Unit,
    onConfirm: (expirationDates: List<Long?>, opened: Boolean) -> Unit,
) {
    var itemDates by remember(product.id) { mutableStateOf(items.ifEmpty { listOf(null) }.map { it?.toLocalDate() }) }
    var opened by remember(product.id) { mutableStateOf(product.opened) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${product.name}") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "${itemDates.size} ${product.quantityUnit.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                itemDates.forEachIndexed { index, date ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        ExpirationDateField(
                            date = date,
                            onDateChanged = { newDate ->
                                itemDates = itemDates.toMutableList().also { it[index] = newDate }
                            },
                            modifier = Modifier.weight(1f),
                            label = "Article ${index + 1}",
                        )
                        IconButton(
                            onClick = { itemDates = itemDates.toMutableList().also { it.removeAt(index) } },
                            enabled = itemDates.size > 1,
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Retirer cet article")
                        }
                    }
                }
                TextButton(onClick = { itemDates = itemDates + null }, modifier = Modifier.padding(top = 4.dp)) {
                    Text("+ Ajouter un article")
                }
                OpenedCheckbox(
                    opened = opened,
                    onOpenedChanged = { opened = it },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(itemDates.map { it?.toEpochMillis() }, opened) }) { Text("Enregistrer") }
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
