package com.rangele.inventory.data.repository

import com.rangele.inventory.data.local.dao.HistoryEntryDao
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(
    private val historyEntryDao: HistoryEntryDao,
) : HistoryRepository {
    override fun observeEntries(): Flow<List<HistoryEntryEntity>> = historyEntryDao.observeAll()
}
