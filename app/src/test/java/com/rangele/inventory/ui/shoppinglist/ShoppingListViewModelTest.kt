package com.rangele.inventory.ui.shoppinglist

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.testutil.FakeInventoryRepository
import com.rangele.inventory.testutil.FakeSettingsRepository
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
        inShoppingList: Boolean = false,
    ) = ProductEntity(
        id = id,
        name = name,
        quantity = quantity,
        unit = QuantityUnit.PIECE.name,
        lowStockThreshold = lowStockThreshold,
        inShoppingList = inShoppingList,
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
            val viewModel = ShoppingListViewModel(repository, FakeSettingsRepository())
            viewModel.uiState.launchIn(backgroundScope)

            assertEquals(
                listOf("Lait"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }

    @Test
    fun `a product is suggested exactly when its quantity is at or below its threshold, zero included`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "StockAboveThreshold", quantity = 5.0, lowStockThreshold = 2.0),
                        product(2, "StockAtThreshold", quantity = 2.0, lowStockThreshold = 2.0),
                        product(3, "StockBelowThreshold", quantity = 1.0, lowStockThreshold = 2.0),
                        product(4, "ZeroStockAboveZeroThreshold", quantity = 0.0, lowStockThreshold = 2.0),
                        product(5, "ZeroStockAtOneThreshold", quantity = 0.0, lowStockThreshold = 1.0),
                        product(6, "ZeroStockAtZeroThreshold", quantity = 0.0, lowStockThreshold = 0.0),
                    ),
                )
            val viewModel = ShoppingListViewModel(repository, FakeSettingsRepository())
            viewModel.uiState.launchIn(backgroundScope)

            assertEquals(
                setOf(
                    "StockAtThreshold",
                    "StockBelowThreshold",
                    "ZeroStockAboveZeroThreshold",
                    "ZeroStockAtOneThreshold",
                    "ZeroStockAtZeroThreshold",
                ),
                viewModel.uiState.value.products
                    .map { it.name }
                    .toSet(),
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
            val viewModel = ShoppingListViewModel(repository, FakeSettingsRepository())
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

    @Test
    fun `threshold mode disabled hides automatic suggestions but keeps manual additions`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        product(1, "Lait", quantity = 1.0, lowStockThreshold = 2.0),
                        product(2, "Beurre", quantity = 5.0, lowStockThreshold = 2.0, inShoppingList = true),
                    ),
                )
            val viewModel =
                ShoppingListViewModel(repository, FakeSettingsRepository(initialThresholdModeEnabled = false))
            viewModel.uiState.launchIn(backgroundScope)

            assertEquals(
                listOf("Beurre"),
                viewModel.uiState.value.products
                    .map { it.name },
            )
        }
}
