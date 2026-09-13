package com.rangele.inventory.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpirationStatusTest {
    private val today = LocalDate.of(2026, 9, 12)

    @Test
    fun `no date means NONE`() {
        assertEquals(ExpirationStatus.NONE, ExpirationStatus.of(null, today))
    }

    @Test
    fun `date before today means EXPIRED`() {
        assertEquals(ExpirationStatus.EXPIRED, ExpirationStatus.of(today.minusDays(1), today))
    }

    @Test
    fun `today counts as SOON, not EXPIRED`() {
        assertEquals(ExpirationStatus.SOON, ExpirationStatus.of(today, today))
    }

    @Test
    fun `six days away is still SOON`() {
        assertEquals(ExpirationStatus.SOON, ExpirationStatus.of(today.plusDays(6), today))
    }

    @Test
    fun `exactly seven days away is OK, not SOON`() {
        assertEquals(ExpirationStatus.OK, ExpirationStatus.of(today.plusDays(7), today))
    }

    @Test
    fun `far in the future is OK`() {
        assertEquals(ExpirationStatus.OK, ExpirationStatus.of(today.plusDays(30), today))
    }
}
