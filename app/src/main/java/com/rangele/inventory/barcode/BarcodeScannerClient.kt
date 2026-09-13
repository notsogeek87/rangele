package com.rangele.inventory.barcode

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Thin wrapper around ML Kit's on-device barcode scanner, analyzing live CameraX frames.
 *
 * The client is created lazily, like [com.rangele.inventory.ocr.ReceiptTextRecognizer]: eagerly
 * building it in [com.rangele.inventory.AppContainer] would touch Play Services on every app
 * launch (and every Robolectric unit test) even when the barcode scanner screen is never opened.
 */
class BarcodeScannerClient {
    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                ).build(),
        )
    }

    /** Analyzes one camera frame, invoking [onBarcodeFound] with the first raw value detected, if any. */
    @ExperimentalGetImage
    fun analyze(
        imageProxy: ImageProxy,
        onBarcodeFound: (String) -> Unit,
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner
            .process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcodeFound)
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }
}
