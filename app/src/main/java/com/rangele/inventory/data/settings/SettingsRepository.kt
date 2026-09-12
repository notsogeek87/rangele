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
}
