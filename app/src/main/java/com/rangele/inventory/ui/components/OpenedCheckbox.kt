package com.rangele.inventory.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Whether the product has already been opened/started — tracked separately from its expiration date. */
@Composable
fun OpenedCheckbox(
    opened: Boolean,
    onOpenedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = opened, onCheckedChange = onOpenedChanged)
        Text("Entamé", modifier = Modifier.padding(start = 4.dp))
    }
}
