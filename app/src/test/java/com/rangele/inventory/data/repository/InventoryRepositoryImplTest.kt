package com.rangele.inventory.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.model.QuantityUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `setting the quantity to zero keeps the product visible with a zero quantity`() =
        runTest {
            val id = repository.insertAsNew("Riz", 2.0, QuantityUnit.PIECE)

            repository.setQuantity(id, 0.0)

            assertEquals(0.0, repository.getById(id)?.quantity ?: -1.0, 0.0)
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
    fun `saveItems with an empty list keeps the product visible with a zero quantity`() =
        runTest {
            val id = repository.insertAsNew("Yaourt", 1.0, QuantityUnit.PIECE)

            repository.saveItems(id, emptyList())

            assertEquals(0.0, repository.getById(id)?.quantity ?: -1.0, 0.0)
            assertTrue(repository.getItems(id).isEmpty())
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

    @Test
    fun `a new product gets the default low stock threshold and is suggested once down to one unit`() =
        runTest {
            // Sans réglage de la part de l'utilisateur : le seuil par défaut suffit à faire
            // remonter le produit en liste de courses quand il n'en reste plus qu'un.
            val id = repository.insertAsNew("Coquillettes", 3.0, QuantityUnit.PIECE)

            assertEquals(1.0, repository.getById(id)?.lowStockThreshold ?: -1.0, 0.0)
            assertTrue(repository.observeLowStockProducts().first().none { it.id == id })

            repository.setQuantity(id, 1.0)

            assertTrue(repository.observeLowStockProducts().first().any { it.id == id })
        }

    @Test
    fun `low stock suggestion follows quantity less than or equal to threshold, including a zero quantity`() =
        runTest {
            val aboveThreshold = repository.insertAsNew("Farine", 5.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(aboveThreshold, 2.0)
            val atThreshold = repository.insertAsNew("Riz", 2.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(atThreshold, 2.0)
            val belowThreshold = repository.insertAsNew("Sucre", 1.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(belowThreshold, 2.0)
            val zeroBelowThreshold = repository.insertAsNew("Sel", 0.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(zeroBelowThreshold, 2.0)
            val zeroAtOneThreshold = repository.insertAsNew("Beurre", 0.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(zeroAtOneThreshold, 1.0)
            val zeroAtZeroThreshold = repository.insertAsNew("Lait", 0.0, QuantityUnit.KILOGRAM)
            repository.updateLowStockThreshold(zeroAtZeroThreshold, 0.0)

            val suggested = repository.observeLowStockProducts().first().map { it.name }

            assertEquals(
                setOf("Riz", "Sucre", "Sel", "Beurre", "Lait"),
                suggested.toSet(),
            )
            assertTrue("Farine" !in suggested)
        }

    @Test
    fun `a manually added product is suggested whatever its stock, until it is removed again`() =
        runTest {
            // Bien approvisionné et très au-dessus de son seuil : seul l'ajout manuel peut le faire
            // figurer en liste de courses.
            val id = repository.insertAsNew("Cafe", 10.0, QuantityUnit.PIECE)
            assertTrue(repository.observeLowStockProducts().first().none { it.id == id })

            repository.setInShoppingList(id, true)
            assertTrue(repository.observeLowStockProducts().first().any { it.id == id })

            repository.setInShoppingList(id, false)
            assertTrue(repository.observeLowStockProducts().first().none { it.id == id })
        }

    @Test
    fun `adding a product to the shopping list leaves its stock and threshold untouched`() =
        runTest {
            val id = repository.insertAsNew("Riz", 4.0, QuantityUnit.PIECE)
            repository.updateLowStockThreshold(id, 2.0)

            repository.setInShoppingList(id, true)

            val product = repository.getById(id)
            assertEquals(4.0, product?.quantity ?: -1.0, 0.0)
            assertEquals(2.0, product?.lowStockThreshold ?: -1.0, 0.0)
            assertTrue(product?.inShoppingList == true)
        }

    @Test
    fun `a product dropping to zero stock stays visible in the inventory and can be suggested`() =
        runTest {
            // "Coquillettes: stock = 1, seuil = 1" already qualifies (1 <= 1); consuming the last
            // pack must not make it disappear from the inventory or the suggestion.
            val id = repository.insertAsNew("Coquillettes", 1.0, QuantityUnit.PIECE)
            repository.updateLowStockThreshold(id, 1.0)
            assertTrue(repository.observeLowStockProducts().first().any { it.id == id })

            repository.adjustQuantity(id, -1.0)

            assertEquals(0.0, repository.getById(id)?.quantity ?: -1.0, 0.0)
            assertTrue(repository.getAllOnce().any { it.id == id })
            assertTrue(repository.observeLowStockProducts().first().any { it.id == id })
        }

    @Test
    fun `changing the threshold immediately updates the suggestion, and restocking above it removes it`() =
        runTest {
            val id = repository.insertAsNew("Cafe", 3.0, QuantityUnit.PIECE)
            assertTrue(repository.observeLowStockProducts().first().none { it.id == id })

            repository.updateLowStockThreshold(id, 3.0)
            assertTrue(repository.observeLowStockProducts().first().any { it.id == id })

            repository.setQuantity(id, 5.0)
            assertTrue(repository.observeLowStockProducts().first().none { it.id == id })
        }

    private suspend fun dates(productId: Long): List<Long?> = repository.getItems(productId).map { it.expirationDate }
}
