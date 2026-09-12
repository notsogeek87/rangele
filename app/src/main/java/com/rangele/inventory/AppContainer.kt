package com.rangele.inventory

import android.content.Context
import androidx.room.Room
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.InventoryRepositoryImpl
import com.rangele.inventory.ocr.ReceiptParser
import com.rangele.inventory.ocr.ReceiptTextRecognizer

/**
 * Minimal hand-rolled dependency container. The app is small and single-module, so a full
 * DI framework would add build complexity (annotation processing, generated graphs) without
 * a real benefit here.
 */
class AppContainer(
    context: Context,
) {
    private val database: AppDatabase =
        Room
            .databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            ).build()

    val inventoryRepository: InventoryRepository = InventoryRepositoryImpl(database.productDao())

    val receiptTextRecognizer: ReceiptTextRecognizer = ReceiptTextRecognizer()

    val receiptParser: ReceiptParser = ReceiptParser()
}
