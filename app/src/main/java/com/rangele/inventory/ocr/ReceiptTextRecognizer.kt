package com.rangele.inventory.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Thin wrapper around ML Kit's on-device Latin text recognizer. */
class ReceiptTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(
        context: Context,
        imageUri: Uri,
    ): String {
        val image = InputImage.fromFilePath(context.applicationContext, imageUri)
        val result = recognizer.process(image).await()
        return result.text
    }
}
