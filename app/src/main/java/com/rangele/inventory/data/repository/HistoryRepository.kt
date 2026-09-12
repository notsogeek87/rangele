package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    /** Withdrawal/deletion journal, most recent first. */
    fun observeEntries(): Flow<List<HistoryEntryEntity>>
}
