package com.rangele.inventory

import android.content.Context
import androidx.room.Room
import com.rangele.inventory.barcode.OpenFoodFactsClient
import com.rangele.inventory.barcode.OpenFoodFactsClientImpl
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.local.MIGRATION_1_2
import com.rangele.inventory.data.local.MIGRATION_2_3
import com.rangele.inventory.data.local.MIGRATION_3_4
import com.rangele.inventory.data.local.MIGRATION_4_5
import com.rangele.inventory.data.repository.CategoryRepository
import com.rangele.inventory.data.repository.CategoryRepositoryImpl
import com.rangele.inventory.data.repository.HistoryRepository
import com.rangele.inventory.data.repository.HistoryRepositoryImpl
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.repository.InventoryRepositoryImpl
import com.rangele.inventory.data.repository.PantryRepository
import com.rangele.inventory.data.repository.PantryRepositoryImpl
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.data.settings.SettingsRepositoryImpl
import com.rangele.inventory.data.settings.settingsDataStore
import com.rangele.inventory.ocr.ReceiptParser
import com.rangele.inventory.ocr.ReceiptTextRecognizer
import com.rangele.inventory.work.ExpirationCheckScheduler
import com.rangele.inventory.work.ExpirationCheckWorker
import com.rangele.inventory.work.ExpirationNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Minimal hand-rolled dependency container. The app is small and single-module, so a full
 * DI framework would add build complexity (annotation processing, generated graphs) without
 * a real benefit here.
 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase =
        Room
            .databaseBuilder(
                appContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    val inventoryRepository: InventoryRepository =
        InventoryRepositoryImpl(database.productDao(), database.historyEntryDao())

    val categoryRepository: CategoryRepository =
        CategoryRepositoryImpl(database.categoryDao(), database.productDao())

    val pantryRepository: PantryRepository =
        PantryRepositoryImpl(database.pantryDao(), database.productDao())

    val historyRepository: HistoryRepository = HistoryRepositoryImpl(database.historyEntryDao())

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext.settingsDataStore)

    val receiptTextRecognizer: ReceiptTextRecognizer = ReceiptTextRecognizer()

    val receiptParser: ReceiptParser = ReceiptParser()

    val openFoodFactsClient: OpenFoodFactsClient = OpenFoodFactsClientImpl()

    private val expirationNotifier = ExpirationNotifier(appContext)

    val expirationCheckScheduler = ExpirationCheckScheduler(appContext)

    val expirationWorkerFactory =
        ExpirationCheckWorker.Factory(inventoryRepository, settingsRepository, expirationNotifier)

    /**
     * Re-applies persisted settings in case the periodic work was never scheduled yet.
     *
     * Called by [RangeleApplication] once the container is assigned, not from an `init` block: this
     * reaches WorkManager, which asks the application back for its Configuration (and therefore for
     * this container) while initializing on demand.
     */
    fun scheduleExpirationChecks() {
        applicationScope.launch {
            expirationCheckScheduler.apply(settingsRepository.settings.first())
        }
    }
}
