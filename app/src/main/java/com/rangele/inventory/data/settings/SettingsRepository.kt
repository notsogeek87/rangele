package com.rangele.inventory.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<NotificationSettings>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setDelayDays(days: Int)

    suspend fun setNotificationTime(
        hour: Int,
        minute: Int,
    )

    /** Epoch millis of the last successful backup export, null if none yet. */
    val lastBackupTimestamp: Flow<Long?>

    suspend fun setLastBackupTimestamp(timestamp: Long)

    /** Apparence choisie dans Paramètres — [ThemeMode.SYSTEM] par défaut. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
