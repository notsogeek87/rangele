package com.rangele.inventory.ui.shoppinglist

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCartCheckout
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.ui.components.EmptyState
import com.rangele.inventory.ui.components.NutriscoreBadge
import com.rangele.inventory.ui.components.ProductAvatar
import com.rangele.inventory.ui.theme.ShapePill
import com.rangele.inventory.ui.theme.Spacing

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
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                shape = ShapePill,
                contentPadding = PaddingValues(vertical = Spacing.lg),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Partager (${uiState.checkedProducts.size})",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) { paddingValues ->
        if (uiState.products.isEmpty()) {
            EmptyState(
                icon = Icons.Default.ShoppingCartCheckout,
                title = "Rien à racheter",
                description =
                    "Aucun produit n'est passé sous son seuil de stock bas. Les produits ajoutés " +
                        "à la main depuis l'inventaire apparaîtront aussi ici.",
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            ProductAvatar(name = product.name, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    product.nutriscore?.let { grade ->
                        NutriscoreBadge(grade = grade, modifier = Modifier.padding(start = Spacing.sm))
                    }
                }
                val reason =
                    if (product.inShoppingList) {
                        "ajouté manuellement"
                    } else {
                        "seuil : ${formatQuantity(product.lowStockThreshold ?: 0.0)}"
                    }
                Text(
                    "Stock actuel : ${formatQuantity(product.quantity)} ${product.quantityUnit.label} ($reason)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
