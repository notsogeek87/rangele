package com.rangele.inventory.ui.addproduct

import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.testutil.FakeCategoryRepository
import com.rangele.inventory.testutil.FakeInventoryRepository
import com.rangele.inventory.testutil.FakePantryRepository
import com.rangele.inventory.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AddProductViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `saving a brand new product inserts it directly`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeInventoryRepository()
            val viewModel = AddProductViewModel(repository, FakeCategoryRepository(), FakePantryRepository())

            viewModel.onNameChanged("Riz basmati")
            viewModel.onQuantityTextChanged("2")
            viewModel.onSaveClicked()

            assertNull(viewModel.uiState.value.mergeSuggestion)
            assertEquals(true, viewModel.uiState.value.isSaved)
            assertEquals(1, repository.getAllOnce().size)
            assertEquals(2.0, repository.getAllOnce().first().quantity, 0.0)
        }

    @Test
    fun `saving a name similar to an existing product proposes a merge`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        ProductEntity(id = 1, name = "Yaourt nature", quantity = 4.0, unit = QuantityUnit.PIECE.name),
                    ),
                )
            val viewModel = AddProductViewModel(repository, FakeCategoryRepository(), FakePantryRepository())

            viewModel.onNameChanged("Yaourts natures")
            viewModel.onQuantityTextChanged("2")
            viewModel.onSaveClicked()

            val suggestion = viewModel.uiState.value.mergeSuggestion
            assertNotNull(suggestion)
            assertEquals("Yaourt nature", suggestion?.name)
            assertFalse(viewModel.uiState.value.isSaved)
        }

    @Test
    fun `confirming the merge adds the quantity to the existing product`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        ProductEntity(id = 1, name = "Yaourt nature", quantity = 4.0, unit = QuantityUnit.PIECE.name),
                    ),
                )
            val viewModel = AddProductViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.onNameChanged("Yaourts natures")
            viewModel.onQuantityTextChanged("2")
            viewModel.onSaveClicked()

            viewModel.onConfirmMergeIntoExisting()

            assertEquals(1, repository.getAllOnce().size)
            assertEquals(6.0, repository.getAllOnce().first().quantity, 0.0)
            assertEquals(true, viewModel.uiState.value.isSaved)
        }

    @Test
    fun `creating a separate product keeps both entries`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakeInventoryRepository(
                    listOf(
                        ProductEntity(id = 1, name = "Yaourt nature", quantity = 4.0, unit = QuantityUnit.PIECE.name),
                    ),
                )
            val viewModel = AddProductViewModel(repository, FakeCategoryRepository(), FakePantryRepository())
            viewModel.onNameChanged("Yaourts natures")
            viewModel.onQuantityTextChanged("2")
            viewModel.onSaveClicked()

            viewModel.onCreateSeparateProduct()

            assertEquals(2, repository.getAllOnce().size)
        }

    @Test
    fun `cannot save with a blank name or non positive quantity`() {
        val repository = FakeInventoryRepository()
        val viewModel = AddProductViewModel(repository, FakeCategoryRepository(), FakePantryRepository())

        viewModel.onQuantityTextChanged("2")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onNameChanged("Riz")
        viewModel.onQuantityTextChanged("0")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onQuantityTextChanged("1")
        assertEquals(true, viewModel.uiState.value.canSave)
    }
}
