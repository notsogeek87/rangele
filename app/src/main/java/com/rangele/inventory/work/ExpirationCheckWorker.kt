package com.rangele.inventory.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rangele.inventory.data.repository.InventoryRepository
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.util.toLocalDate
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Runs once a day (see [ExpirationCheckScheduler]) to flag products expiring within the Settings delay. */
class ExpirationCheckWorker(
    context: Context,
    params: WorkerParameters,
    private val inventoryRepository: InventoryRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: ExpirationNotifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.enabled) return Result.success()

        val today = LocalDate.now()
        val horizon = today.plusDays(settings.delayDays.toLong())
        val expiringSoon =
            inventoryRepository.getAllOnce().filter { product ->
                val expirationDate = product.expirationDate?.toLocalDate() ?: return@filter false
                !expirationDate.isAfter(horizon)
            }

        notifier.notifyExpiringProducts(expiringSoon)
        return Result.success()
    }

    class Factory(
        private val inventoryRepository: InventoryRepository,
        private val settingsRepository: SettingsRepository,
        private val notifier: ExpirationNotifier,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? =
            if (workerClassName == ExpirationCheckWorker::class.java.name) {
                ExpirationCheckWorker(appContext, workerParameters, inventoryRepository, settingsRepository, notifier)
            } else {
                null
            }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "expiration_check"
    }
}
