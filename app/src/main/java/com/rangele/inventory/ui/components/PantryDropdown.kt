package com.rangele.inventory.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rangele.inventory.data.local.entity.PantryEntity

private const val NO_PANTRY_LABEL = "Aucun placard"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryDropdown(
    pantries: List<PantryEntity>,
    selectedPantryId: Long?,
    onPantrySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Placard",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = pantries.firstOrNull { it.id == selectedPantryId }?.name ?: NO_PANTRY_LABEL
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            DropdownMenuItem(
                text = { Text(NO_PANTRY_LABEL) },
                onClick = {
                    onPantrySelected(null)
                    expanded = false
                },
            )
            pantries.forEach { pantry ->
                DropdownMenuItem(
                    text = { Text(pantry.name) },
                    onClick = {
                        onPantrySelected(pantry.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
