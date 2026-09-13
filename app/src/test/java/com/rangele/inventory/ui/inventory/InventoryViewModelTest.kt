package com.rangele.inventory.ui.inventory

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.testutil.FakeCategoryRepository
import com.rangele.inventory.testutil.FakeInventoryRepository
import com.rangele.inventory.testutil.FakePantryRepository
import com.rangele.inventory.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InventoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun product(
        id: Long,
        name: String,
        quantity: Double = 1.0,
        unit: QuantityUnit = QuantityUnit.PIECE,
        category: String? = null,
        expirationDate: Long? = null,
    ) = ProductEntity(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit.name,
        category = category,
        expirationDate = expirationDate,
    )

    /** Keeps the WhileSubscribed StateFlow active for the duration of a test. */
    private fun InventoryViewModel.collectInBackground(scope: kotlinx.coroutines.CoroutineScope) {
        uiState.launchIn(scope)
    }

    @Test
    fun `products are exposed sorted alphabetically`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(product(1, "Yaourt"), product(2, "Ananas"), product(3, "Beurre")),
                )
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            assertEquals(
                listOf("Ananas", "Beurre", "Yaourt"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `search query filters the product list`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(product(1, "Lait demi-ecreme"), product(2, "Lait entier"), product(3, "Farine")),
                )
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onSearchQueryChanged("lait")

            assertEquals(2, viewModel.uiState.value.products.size)
            assertTrue(
                viewModel.uiState.value.products
                    .all { it.name.contains("Lait", ignoreCase = true) },
            )
        }

    @Test
    fun `increment adds the unit step to the quantity`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes", quantity = 2.0)))
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onIncrement(
                viewModel.uiState.value.products
                    .first(),
            )

            assertEquals(
                3.0,
                viewModel.uiState.value.products
                    .first()
                    .quantity,
                0.0,
            )
        }

    @Test
    fun `decrement to zero keeps the product in the inventory`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes", quantity = 1.0)))
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onDecrement(
                viewModel.uiState.value.products
                    .first(),
            )

            // Un produit épuisé reste visible : c'est ce qui permet de le retrouver, de le
            // réapprovisionner et de le voir remonter dans la liste de courses suggérée.
            assertEquals(
                0.0,
                viewModel.uiState.value.products
                    .single()
                    .quantity,
                0.0,
            )
        }

    @Test
    fun `category filter narrows the product list to that category`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "Riz", category = "Placard"),
                        product(2, "Yaourt", category = "Frigo"),
                        product(3, "Farine", category = "Placard"),
                    ),
                )
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onCategoryFilterChanged("Placard")

            assertEquals(
                listOf("Farine", "Riz"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `sorting by expiration puts products without a date last`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "Sans date"),
                        product(2, "Perime bientot", expirationDate = 2_000L),
                        product(3, "Perime plus tard", expirationDate = 3_000L),
                    ),
                )
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onSortModeChanged(SortMode.EXPIRATION)

            assertEquals(
                listOf("Perime bientot", "Perime plus tard", "Sans date"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `delete removes the product from the list`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes")))
            val viewModel = InventoryViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.collectInBackground(backgroundScope)

            viewModel.onDelete(
                viewModel.uiState.value.products
                    .first(),
            )

            assertTrue(
                viewModel.uiState.value.products
                    .isEmpty(),
            )
        }
}
