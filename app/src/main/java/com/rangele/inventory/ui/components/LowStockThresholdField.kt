package com.rangele.inventory.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Editable low-stock threshold: an optional non-negative integer, compared to the product's
 * quantity as `quantity <= threshold` (see [com.rangele.inventory.data.local.dao.ProductDao.observeLowStock]).
 * Blank disables the suggestion for this product.
 */
@Composable
fun LowStockThresholdField(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = { input -> onTextChanged(input.filter { it.isDigit() }) },
            label = { Text("Seuil de stock bas") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text(
            text = "Le produit sera suggéré lorsque le stock sera inférieur ou égal à ce seuil.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
