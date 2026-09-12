package com.rangele.inventory

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class RangeleApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Default androidx.startup init is disabled in the manifest so this can hand it our WorkerFactory.
        WorkManager.initialize(
            this,
            Configuration
                .Builder()
                .setWorkerFactory(container.expirationWorkerFactory)
                .build(),
        )
    }
}
