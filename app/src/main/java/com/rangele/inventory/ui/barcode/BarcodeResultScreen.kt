package com.rangele.inventory.ui.barcode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rangele.inventory.barcode.OffProduct
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.ui.components.CategoryDropdown
import com.rangele.inventory.ui.components.UnitDropdown
import com.rangele.inventory.ui.theme.ShapeSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeResultScreen(
    viewModel: BarcodeScanViewModel,
    onBackClick: () -> Unit,
    onDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produit scanné") },
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
            when (val lookup = uiState.lookup) {
                is BarcodeLookupState.Scanning -> Unit

                is BarcodeLookupState.Loading ->
                    CenteredMessage {
                        CircularProgressIndicator()
                        Text(
                            "Recherche du produit sur Open Food Facts…",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                is BarcodeLookupState.AlreadyInInventory ->
                    AlreadyInInventoryContent(
                        existing = lookup.existing,
                        onAdjust = viewModel::onAdjustExistingQuantity,
                        onDone = onDone,
                    )

                is BarcodeLookupState.Found ->
                    ProductForm(
                        viewModel = viewModel,
                        saveLabel = "Ajouter à l'inventaire",
                        offProduct = lookup.product,
                    )

                is BarcodeLookupState.NotFound ->
                    ProductForm(
                        viewModel = viewModel,
                        saveLabel = "Créer le produit",
                        headerMessage =
                            "Ce produit n'est pas référencé sur Open Food Facts. Vous pouvez tout de même " +
                                "le créer : le code-barres sera conservé pour le reconnaître la prochaine fois.",
                    )

                is BarcodeLookupState.Error ->
                    ProductForm(
                        viewModel = viewModel,
                        saveLabel = "Créer le produit",
                        headerMessage =
                            "Impossible de contacter Open Food Facts (vérifiez votre connexion). " +
                                "Vous pouvez réessayer, ou créer le produit manuellement.",
                        onRetry = viewModel::onRetryLookup,
                    )
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlreadyInInventoryContent(
    existing: ProductEntity,
    onAdjust: (Double) -> Unit,
    onDone: () -> Unit,
) {
    var addText by remember(existing.id) { mutableStateOf("") }
    var removeText by remember(existing.id) { mutableStateOf("") }
    val addAmount = addText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    val removeAmount = removeText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            "Produit déjà présent",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(existing.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            "Nombre actuel : ${formatPlainQuantity(existing.quantity)} ${existing.quantityUnit.label}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = addText,
            onValueChange = { addText = it },
            label = { Text("Combien en ajouter ?") },
            suffix = { Text(existing.quantityUnit.label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                addAmount?.let {
                    onAdjust(it)
                    addText = ""
                }
            },
            enabled = addAmount != null,
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        ) {
            Text("Ajouter")
        }

        OutlinedTextField(
            value = removeText,
            onValueChange = { removeText = it },
            label = { Text("Combien retirer ?") },
            suffix = { Text(existing.quantityUnit.label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                removeAmount?.let {
                    onAdjust(-it)
                    removeText = ""
                }
            },
            enabled = removeAmount != null,
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Retirer")
        }

        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
            Text("Terminé")
        }
    }
}

@Composable
private fun ProductForm(
    viewModel: BarcodeScanViewModel,
    saveLabel: String,
    offProduct: OffProduct? = null,
    headerMessage: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (offProduct != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (offProduct.imageUrl != null) {
                        AsyncImage(
                            model = offProduct.imageUrl,
                            contentDescription = offProduct.name,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .padding(bottom = 8.dp),
                        )
                    }
                    if (offProduct.brand != null) {
                        Text(offProduct.brand, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (offProduct.packageFormat != null) {
                        Text(
                            "Format : ${offProduct.packageFormat}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (headerMessage != null) {
            Text(
                text = headerMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = if (offProduct != null) 12.dp else 0.dp, bottom = 4.dp),
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Réessayer")
                }
            }
        }

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChanged,
            label = { Text("Nom du produit") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

        CategoryDropdown(
            categories = uiState.availableCategories,
            selectedCategory = uiState.category,
            onCategorySelected = viewModel::onCategoryChanged,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        Button(
            onClick = viewModel::onSaveClicked,
            enabled = uiState.canSave,
            shape = ShapeSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
        ) {
            Text(saveLabel)
        }
    }
}

private fun formatPlainQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
