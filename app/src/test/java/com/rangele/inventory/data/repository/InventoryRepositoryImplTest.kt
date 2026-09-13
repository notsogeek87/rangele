package com.rangele.inventory.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.model.QuantityUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Uses the real DAOs (in-memory Room via Robolectric) since the history side effect must match the actual SQL. */
@RunWith(RobolectricTestRunner::class)
class InventoryRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: InventoryRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = InventoryRepositoryImpl(database.productDao(), database.historyEntryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `decreasing the quantity logs a withdrawal`() =
        runTest {
            val id = repository.insertAsNew("Riz", 5.0, QuantityUnit.PIECE)

            repository.setQuantity(id, 2.0)

            val entries = database.historyEntryDao().getAllOnce()
            assertEquals(1, entries.size)
            assertEquals("Riz", entries.first().productName)
            assertEquals(3.0, entries.first().quantityRemoved, 0.0)
        }

    @Test
    fun `increasing the quantity does not log anything`() =
        runTest {
            val id = repository.insertAsNew("Riz", 5.0, QuantityUnit.PIECE)

            repository.setQuantity(id, 8.0)

            assertTrue(database.historyEntryDao().getAllOnce().isEmpty())
        }

    @Test
    fun `setting the quantity to zero deletes the product`() =
        runTest {
            val id = repository.insertAsNew("Riz", 2.0, QuantityUnit.PIECE)

            repository.setQuantity(id, 0.0)

            assertNull(repository.getById(id))
            val entries = database.historyEntryDao().getAllOnce()
            assertEquals(1, entries.size)
            assertEquals(2.0, entries.first().quantityRemoved, 0.0)
        }

    @Test
    fun `deleting a product logs its remaining quantity as a withdrawal`() =
        runTest {
            val id = repository.insertAsNew("Farine", 4.0, QuantityUnit.PIECE)

            repository.deleteProduct(id)

            val entries = database.historyEntryDao().getAllOnce()
            assertEquals(1, entries.size)
            assertEquals("Farine", entries.first().productName)
            assertEquals(4.0, entries.first().quantityRemoved, 0.0)
        }

    @Test
    fun `updateDetails changes the expiration date and opened status without touching quantity`() =
        runTest {
            val id = repository.insertAsNew("Fromage", 1.0, QuantityUnit.PIECE)

            repository.updateDetails(id, expirationDate = 123456789L, opened = true)

            val updated = repository.getById(id)
            assertEquals(123456789L, updated?.expirationDate)
            assertTrue(updated?.opened == true)
            assertEquals(1.0, updated?.quantity ?: 0.0, 0.0)
        }
}
