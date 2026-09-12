package com.rangele.inventory.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rangele.inventory.data.settings.NotificationSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules/cancels the daily [ExpirationCheckWorker] run, matching the current Settings. */
class ExpirationCheckScheduler(
    private val context: Context,
) {
    fun apply(settings: NotificationSettings) {
        if (settings.enabled) {
            scheduleDaily(settings.hour, settings.minute)
        } else {
            cancel()
        }
    }

    private fun scheduleDaily(
        hour: Int,
        minute: Int,
    ) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val initialDelay = Duration.between(now, next).toMillis()

        val request =
            PeriodicWorkRequestBuilder<ExpirationCheckWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                ExpirationCheckWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(ExpirationCheckWorker.UNIQUE_WORK_NAME)
    }
}
