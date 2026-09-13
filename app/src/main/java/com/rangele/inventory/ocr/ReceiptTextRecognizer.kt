package com.rangele.inventory.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.roundToInt

private const val PDF_MIME_TYPE = "application/pdf"

/**
 * PDF pages are measured in points (1/72 inch), so rendering one at its natural size hands the
 * recognizer a ~72 dpi image — far too coarse for the small print on a receipt.
 */
private const val PDF_RENDER_WIDTH_PX = 1600

/**
 * Thin wrapper around ML Kit's on-device Latin text recognizer.
 *
 * The client is created lazily rather than in the constructor: [AppContainer][com.rangele.inventory.AppContainer]
 * builds this eagerly at app startup, and eagerly creating the ML Kit client there touches Play Services on
 * every app launch (and every Robolectric unit test) even when no ticket is ever scanned.
 */
class ReceiptTextRecognizer {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** [documentUri] is a photo or a PDF — an e-receipt downloaded from a shop is usually a PDF. */
    suspend fun recognizeText(
        context: Context,
        documentUri: Uri,
    ): String {
        val appContext = context.applicationContext
        return if (isPdf(appContext, documentUri)) {
            recognizePdf(appContext, documentUri)
        } else {
            recognize(InputImage.fromFilePath(appContext, documentUri))
        }
    }

    private fun isPdf(
        context: Context,
        uri: Uri,
    ): Boolean =
        context.contentResolver.getType(uri) == PDF_MIME_TYPE ||
            uri.toString().endsWith(".pdf", ignoreCase = true)

    private suspend fun recognize(image: InputImage): String = recognizer.process(image).await().text

    /** ML Kit only reads bitmaps, so every page is rasterised first. */
    private suspend fun recognizePdf(
        context: Context,
        uri: Uri,
    ): String {
        val descriptor =
            context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("Ce fichier PDF n'a pas pu être ouvert.")
        val text = StringBuilder()
        descriptor.use { file ->
            PdfRenderer(file).use { renderer ->
                for (pageIndex in 0 until renderer.pageCount) {
                    val page = renderPage(renderer, pageIndex)
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(recognize(InputImage.fromBitmap(page, 0)))
                    page.recycle()
                }
            }
        }
        return text.toString()
    }

    private fun renderPage(
        renderer: PdfRenderer,
        pageIndex: Int,
    ): Bitmap =
        renderer.openPage(pageIndex).use { page ->
            val width = page.width.coerceAtLeast(1)
            val scale = max(1f, PDF_RENDER_WIDTH_PX.toFloat() / width)
            val bitmap =
                Bitmap.createBitmap(
                    (width * scale).roundToInt(),
                    (page.height.coerceAtLeast(1) * scale).roundToInt(),
                    Bitmap.Config.ARGB_8888,
                )
            // PdfRenderer paints the page content only: without this the background stays transparent,
            // which flattens to black and drowns the text.
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
}
