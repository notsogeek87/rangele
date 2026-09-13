package com.rangele.inventory.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Optional, clearable expiration-date picker built on Material 3's [DatePicker]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationDateField(
    date: LocalDate?,
    onDateChanged: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date de péremption (optionnel)",
) {
    var showPicker by remember { mutableStateOf(false) }

    // enabled = false so the field's own focus/cursor handling can't swallow the tap meant for
    // the outer clickable (a readOnly field alone is unreliable here); colors restore its normal
    // look since "disabled" would otherwise render it greyed out.
    Box(
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { showPicker = true },
    ) {
        OutlinedTextField(
            value = date?.format(DISPLAY_FORMAT) ?: "",
            onValueChange = {},
            enabled = false,
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                if (date != null) {
                    IconButton(onClick = { onDateChanged(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer la date")
                    }
                } else {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                }
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showPicker) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = date?.let { it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateChanged(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annuler") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
