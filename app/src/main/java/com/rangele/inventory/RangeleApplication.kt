package com.rangele.inventory

import android.app.Application
import androidx.work.Configuration

class RangeleApplication :
    Application(),
    Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scheduleExpirationChecks()
    }

    // On-demand initialization: WorkManager reads this the first time WorkManager.getInstance(context) is
    // called, instead of us calling WorkManager.initialize() eagerly (which throws if called more than once
    // per process — e.g. across Robolectric test classes, which each spin up a fresh Application). The
    // default androidx.startup init is disabled in the manifest, which this override requires.
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(container.expirationWorkerFactory)
                .build()
}
