package com.rangele.inventory.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<NotificationSettings> =
        dataStore.data.map { prefs ->
            NotificationSettings(
                enabled = prefs[Keys.ENABLED] ?: false,
                delayDays = prefs[Keys.DELAY_DAYS] ?: NotificationSettings.DEFAULT_DELAY_DAYS,
                hour = prefs[Keys.HOUR] ?: NotificationSettings.DEFAULT_HOUR,
                minute = prefs[Keys.MINUTE] ?: NotificationSettings.DEFAULT_MINUTE,
            )
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    override suspend fun setDelayDays(days: Int) {
        dataStore.edit { it[Keys.DELAY_DAYS] = days }
    }

    override suspend fun setNotificationTime(
        hour: Int,
        minute: Int,
    ) {
        dataStore.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    override val lastBackupTimestamp: Flow<Long?> =
        dataStore.data.map { prefs -> prefs[Keys.LAST_BACKUP_AT] }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { it[Keys.LAST_BACKUP_AT] = timestamp }
    }

    override val themeMode: Flow<ThemeMode> =
        dataStore.data.map { prefs -> ThemeMode.fromStorageValue(prefs[Keys.THEME_MODE]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override val thresholdModeEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.THRESHOLD_MODE_ENABLED] ?: true }

    override suspend fun setThresholdModeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.THRESHOLD_MODE_ENABLED] = enabled }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("notifications_enabled")
        val DELAY_DAYS = intPreferencesKey("expiration_delay_days")
        val HOUR = intPreferencesKey("notification_hour")
        val MINUTE = intPreferencesKey("notification_minute")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THRESHOLD_MODE_ENABLED = booleanPreferencesKey("threshold_mode_enabled")
    }
}
