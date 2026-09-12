package com.rangele.inventory.ui.shoppinglist

import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.ui.theme.ShapeSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste de courses suggérée") },
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
                onClick = {
                    val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.buildShareText())
                        }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager la liste de courses"))
                },
                enabled = uiState.checkedProducts.isNotEmpty(),
                shape = ShapeSmall,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(" Partager (${uiState.checkedProducts.size})")
            }
        },
    ) { paddingValues ->
        if (uiState.products.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Aucun produit sous son seuil de stock bas pour le moment.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.products, key = { it.id }) { product ->
                    ShoppingListRow(
                        product = product,
                        checked = product.id in uiState.checkedIds,
                        onToggle = { viewModel.onToggle(product) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(
    product: ProductEntity,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Stock actuel : ${formatQuantity(product.quantity)} ${product.quantityUnit.label} " +
                        "(seuil : ${formatQuantity(product.lowStockThreshold ?: 0.0)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
