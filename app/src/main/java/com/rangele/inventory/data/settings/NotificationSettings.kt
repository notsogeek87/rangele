package com.rangele.inventory.data.settings

/** User-configurable behavior of the daily expiration check (see Settings screen). */
data class NotificationSettings(
    val enabled: Boolean = false,
    /** Days before a product's expiration date at which it starts being flagged. */
    val delayDays: Int = DEFAULT_DELAY_DAYS,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
) {
    companion object {
        const val DEFAULT_DELAY_DAYS = 3
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }
}
