package com.rangele.inventory.testutil

import com.rangele.inventory.data.settings.NotificationSettings
import com.rangele.inventory.data.settings.SettingsRepository
import com.rangele.inventory.data.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for [SettingsRepository]. */
class FakeSettingsRepository(
    initialThresholdModeEnabled: Boolean = true,
) : SettingsRepository {
    private val notificationSettings = MutableStateFlow(NotificationSettings())
    private val backupTimestamp = MutableStateFlow<Long?>(null)
    private val theme = MutableStateFlow(ThemeMode.SYSTEM)
    private val thresholdMode = MutableStateFlow(initialThresholdModeEnabled)

    override val settings: Flow<NotificationSettings> = notificationSettings

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationSettings.value = notificationSettings.value.copy(enabled = enabled)
    }

    override suspend fun setDelayDays(days: Int) {
        notificationSettings.value = notificationSettings.value.copy(delayDays = days)
    }

    override suspend fun setNotificationTime(
        hour: Int,
        minute: Int,
    ) {
        notificationSettings.value = notificationSettings.value.copy(hour = hour, minute = minute)
    }

    override val lastBackupTimestamp: Flow<Long?> = backupTimestamp

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        backupTimestamp.value = timestamp
    }

    override val themeMode: Flow<ThemeMode> = theme

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override val thresholdModeEnabled: Flow<Boolean> = thresholdMode

    override suspend fun setThresholdModeEnabled(enabled: Boolean) {
        thresholdMode.value = enabled
    }
}
