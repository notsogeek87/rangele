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
        repository =
            InventoryRepositoryImpl(database.productDao(), database.historyEntryDao(), database.productItemDao())
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

    @Test
    fun `inserting a discrete-unit product with a date creates one item per unit`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 3.0, QuantityUnit.PIECE, expirationDate = 1_000L)

            assertEquals(listOf(1_000L, 1_000L, 1_000L), dates(id))
            assertEquals(1_000L, repository.getById(id)?.expirationDate)
        }

    @Test
    fun `inserting a discrete-unit product already opened marks every item opened`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 2.0, QuantityUnit.PIECE, opened = true)

            assertTrue(repository.getItems(id).all { it.opened })
            assertTrue(repository.getById(id)?.opened == true)
        }

    @Test
    fun `inserting a continuous-unit product never creates items`() =
        runTest {
            val id = repository.insertAsNew("Farine", 1.5, QuantityUnit.KILOGRAM, expirationDate = 1_000L)

            assertTrue(repository.getItems(id).isEmpty())
        }

    @Test
    fun `saveItems replaces the items and derives the product's quantity, cached date and opened status`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 2.0, QuantityUnit.PIECE)

            repository.saveItems(
                id,
                listOf(
                    ItemDetails(expirationDate = 3_000L, opened = false),
                    ItemDetails(expirationDate = null, opened = true),
                    ItemDetails(expirationDate = 1_000L, opened = false),
                ),
            )

            assertEquals(listOf(3_000L, null, 1_000L), dates(id))
            val updated = repository.getById(id)
            assertEquals(3.0, updated?.quantity ?: 0.0, 0.0)
            assertEquals(1_000L, updated?.expirationDate)
            // One item is opened, so the product-level cache reflects it even though the others aren't.
            assertTrue(updated?.opened == true)
        }

    @Test
    fun `saveItems with an empty list deletes the product like setting the quantity to zero`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 1.0, QuantityUnit.PIECE)

            repository.saveItems(id, emptyList())

            assertNull(repository.getById(id))
        }

    @Test
    fun `decreasing quantity removes the soonest-expiring items first`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 2.0, QuantityUnit.PIECE)
            repository.saveItems(
                id,
                listOf(ItemDetails(3_000L, opened = false), ItemDetails(1_000L, opened = false)),
            )

            repository.setQuantity(id, 1.0)

            assertEquals(listOf(3_000L), dates(id))
            assertEquals(3_000L, repository.getById(id)?.expirationDate)
        }

    @Test
    fun `decreasing quantity past the only opened item clears the product's opened cache`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 2.0, QuantityUnit.PIECE)
            repository.saveItems(
                id,
                listOf(ItemDetails(1_000L, opened = true), ItemDetails(3_000L, opened = false)),
            )

            // FEFO removes the soonest-expiring item first: here the opened one (1_000L).
            repository.setQuantity(id, 1.0)

            assertTrue(repository.getById(id)?.opened == false)
        }

    @Test
    fun `incrementing an existing discrete-unit product tags only the new units with the given date`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 1.0, QuantityUnit.PIECE, expirationDate = 1_000L)

            repository.incrementExisting(id, 1.0, expirationDate = 2_000L)

            assertEquals(listOf(1_000L, 2_000L), dates(id).sortedBy { it })
            assertEquals(1_000L, repository.getById(id)?.expirationDate)
        }

    private suspend fun dates(productId: Long): List<Long?> = repository.getItems(productId).map { it.expirationDate }
}
