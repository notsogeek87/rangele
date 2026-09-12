package com.rangele.inventory.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Display status for a product's expiration date in the inventory list. */
enum class ExpirationStatus {
    /** No expiration date set. */
    NONE,

    /** Set, but neither imminent nor passed. */
    OK,

    /** Less than [SOON_THRESHOLD_DAYS] days away (today included). */
    SOON,

    /** Already past. */
    EXPIRED,
    ;

    companion object {
        const val SOON_THRESHOLD_DAYS = 7L

        fun of(
            expirationDate: LocalDate?,
            today: LocalDate = LocalDate.now(),
        ): ExpirationStatus =
            when {
                expirationDate == null -> NONE
                expirationDate.isBefore(today) -> EXPIRED
                expirationDate.isBefore(today.plusDays(SOON_THRESHOLD_DAYS)) -> SOON
                else -> OK
            }
    }
}

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
