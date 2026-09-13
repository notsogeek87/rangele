package com.rangele.inventory.ui.scan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Lets the user pick an already-saved receipt — a photo or a PDF — from anywhere on the device.
 *
 * ACTION_GET_CONTENT (rather than ACTION_PICK on the photo gallery alone) opens the system document
 * browser, so the receipt can come from Files, Downloads, Drive, WhatsApp… and not only from the
 * default gallery albums. [Intent.EXTRA_MIME_TYPES] narrows that browser down to what the OCR can
 * actually read (see [com.rangele.inventory.ocr.ReceiptTextRecognizer]).
 */
internal fun receiptDocumentPickerIntent(): Intent =
    Intent(Intent.ACTION_GET_CONTENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*")
        .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))

/** Returns a callback that opens the picker built by [receiptDocumentPickerIntent]. */
@Composable
internal fun rememberReceiptDocumentPicker(
    onPicked: (Uri) -> Unit,
    onCancelled: () -> Unit = {},
): () -> Unit {
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) onPicked(uri) else onCancelled()
        }
    return { launcher.launch(receiptDocumentPickerIntent()) }
}

/**
 * Import shortcut reached straight from the inventory, skipping the camera screen: it opens the
 * picker as soon as it is shown, then hands the chosen file to the same review flow as a capture.
 */
@Composable
fun ReceiptImportScreen(
    viewModel: ScanViewModel,
    onImported: () -> Unit,
    onCancelled: () -> Unit,
) {
    val context = LocalContext.current
    val pickReceiptDocument =
        rememberReceiptDocumentPicker(
            onPicked = { uri ->
                viewModel.onPhotoCaptured(context, uri)
                onImported()
            },
            onCancelled = onCancelled,
        )

    LaunchedEffect(Unit) { pickReceiptDocument() }

    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
