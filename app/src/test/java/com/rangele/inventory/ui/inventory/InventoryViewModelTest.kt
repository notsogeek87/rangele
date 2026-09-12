package com.rangele.inventory.ui.inventory

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.testutil.FakeInventoryRepository
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
    ) = ProductEntity(id = id, name = name, quantity = quantity, unit = unit.name)

    /** Keeps the WhileSubscribed StateFlow active for the duration of a test. */
    private fun InventoryViewModel.collectInBackground(scope: kotlinx.coroutines.CoroutineScope) {
        uiState.launchIn(scope)
    }

    @Test
    fun `products are exposed sorted alphabetically`() =
        runTest {
            val repository =
                FakeInventoryRepository(
                    listOf(product(1, "Yaourt"), product(2, "Ananas"), product(3, "Beurre")),
                )
            val viewModel = InventoryViewModel(repository)
            viewModel.collectInBackground(backgroundScope)

            assertEquals(
                listOf("Ananas", "Beurre", "Yaourt"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `search query filters the product list`() =
        runTest {
            val repository =
                FakeInventoryRepository(
                    listOf(product(1, "Lait demi-ecreme"), product(2, "Lait entier"), product(3, "Farine")),
                )
            val viewModel = InventoryViewModel(repository)
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
        runTest {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes", quantity = 2.0)))
            val viewModel = InventoryViewModel(repository)
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
    fun `decrement never pushes the quantity below zero`() =
        runTest {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes", quantity = 0.0)))
            val viewModel = InventoryViewModel(repository)
            viewModel.collectInBackground(backgroundScope)

            viewModel.onDecrement(
                viewModel.uiState.value.products
                    .first(),
            )

            assertEquals(
                0.0,
                viewModel.uiState.value.products
                    .first()
                    .quantity,
                0.0,
            )
        }

    @Test
    fun `delete removes the product from the list`() =
        runTest {
            val repository = FakeInventoryRepository(listOf(product(1, "Pommes")))
            val viewModel = InventoryViewModel(repository)
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
