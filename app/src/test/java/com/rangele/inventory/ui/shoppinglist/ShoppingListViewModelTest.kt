package com.rangele.inventory.ui.shoppinglist

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

class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun product(
        id: Long,
        name: String,
        quantity: Double,
        lowStockThreshold: Double?,
    ) = ProductEntity(
        id = id,
        name = name,
        quantity = quantity,
        unit = QuantityUnit.PIECE.name,
        lowStockThreshold = lowStockThreshold,
    )

    @Test
    fun `only products below their own threshold are suggested`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "Lait", quantity = 1.0, lowStockThreshold = 2.0),
                        product(2, "Riz", quantity = 5.0, lowStockThreshold = 2.0),
                        product(3, "Farine", quantity = 1.0, lowStockThreshold = null),
                    ),
                )
            val viewModel = ShoppingListViewModel(repository)
            viewModel.uiState.launchIn(backgroundScope)

            assertEquals(
                listOf("Lait"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `share text only includes checked products`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "Lait", quantity = 1.0, lowStockThreshold = 2.0),
                        product(2, "Beurre", quantity = 0.0, lowStockThreshold = 1.0),
                    ),
                )
            val viewModel = ShoppingListViewModel(repository)
            viewModel.uiState.launchIn(backgroundScope)

            viewModel.onToggle(
                viewModel.uiState.value.products
                    .first { it.name == "Lait" },
            )

            assertEquals("- Lait", viewModel.buildShareText())
            assertTrue(
                viewModel.uiState.value.checkedProducts
                    .none { it.name == "Beurre" },
            )
        }
}
