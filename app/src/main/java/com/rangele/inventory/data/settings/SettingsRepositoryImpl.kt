package com.rangele.inventory.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    private object Keys {
        val ENABLED = booleanPreferencesKey("notifications_enabled")
        val DELAY_DAYS = intPreferencesKey("expiration_delay_days")
        val HOUR = intPreferencesKey("notification_hour")
        val MINUTE = intPreferencesKey("notification_minute")
    }
}
