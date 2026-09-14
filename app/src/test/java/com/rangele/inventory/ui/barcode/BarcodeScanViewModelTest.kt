package com.rangele.inventory.ui.barcode

import com.rangele.inventory.barcode.OffLookupResult
import com.rangele.inventory.barcode.OffProduct
import com.rangele.inventory.barcode.OffProductRepository
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.testutil.FakeCategoryRepository
import com.rangele.inventory.testutil.FakeInventoryRepository
import com.rangele.inventory.testutil.FakeOffProductRepository
import com.rangele.inventory.testutil.FakePantryRepository
import com.rangele.inventory.testutil.MainDispatcherRule
import com.rangele.inventory.util.toEpochMillis
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class BarcodeScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a barcode already in the inventory is reported as already present`() =
        runTest(mainDispatcherRule.dispatcher) {
            val existing = productWithBarcode("111")
            val repository = FakeInventoryRepository(listOf(existing))
            val viewModel =
                BarcodeScanViewModel(
                    repository,
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    FakeOffProductRepository(),
                )

            viewModel.onBarcodeDetected("111")

            val lookup = viewModel.uiState.value.lookup
            assertTrue(lookup is BarcodeLookupState.AlreadyInInventory)
            assertEquals("Eau minérale", (lookup as BarcodeLookupState.AlreadyInInventory).existing.name)
        }

    @Test
    fun `incrementing an already present product updates its quantity and returns to the list`() =
        runTest(mainDispatcherRule.dispatcher) {
            val existing = productWithBarcode("111")
            val repository = FakeInventoryRepository(listOf(existing))
            val viewModel =
                BarcodeScanViewModel(
                    repository,
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    FakeOffProductRepository(),
                )
            viewModel.onBarcodeDetected("111")

            viewModel.onAdjustExistingQuantity(1.0)

            assertTrue(viewModel.uiState.value.isSaved)
            assertEquals(3.0, repository.getAllOnce().first().quantity, 0.0)
        }

    @Test
    fun `removing the last unit of an already present product leaves it at zero stock`() =
        runTest(mainDispatcherRule.dispatcher) {
            val existing = productWithBarcode("111").copy(quantity = 1.0)
            val repository = FakeInventoryRepository(listOf(existing))
            val viewModel =
                BarcodeScanViewModel(
                    repository,
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    FakeOffProductRepository(),
                )
            viewModel.onBarcodeDetected("111")

            viewModel.onAdjustExistingQuantity(-1.0)

            assertTrue(viewModel.uiState.value.isSaved)
            assertEquals(0.0, repository.getAllOnce().single().quantity, 0.0)
        }

    @Test
    fun `a product found on Open Food Facts prefills the editable form`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "222", name = "Nutella", brand = "Ferrero", packageFormat = "400 g")
            val client = FakeOffProductRepository(mapOf("222" to OffLookupResult.Found(offProduct)))
            val viewModel =
                BarcodeScanViewModel(
                    FakeInventoryRepository(),
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    client,
                )

            viewModel.onBarcodeDetected("222")

            val state = viewModel.uiState.value
            assertTrue(state.lookup is BarcodeLookupState.Found)
            assertEquals("Nutella", state.name)
            assertEquals("222", client.lastRequestedBarcode)
        }

    @Test
    fun `forcing a refresh bypasses the cache and updates the found product`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "222", name = "Nutella")
            val refreshedProduct = OffProduct(barcode = "222", name = "Nutella (actualisé)")
            val client = FakeOffProductRepository(mapOf("222" to OffLookupResult.Found(offProduct)))
            val viewModel =
                BarcodeScanViewModel(
                    FakeInventoryRepository(),
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    client,
                )
            viewModel.onBarcodeDetected("222")

            client.setResult("222", OffLookupResult.Found(refreshedProduct))
            viewModel.onForceRefresh()

            assertEquals(1, client.refreshCallCount)
            val lookup = viewModel.uiState.value.lookup
            assertTrue(lookup is BarcodeLookupState.Found)
            assertEquals("Nutella (actualisé)", (lookup as BarcodeLookupState.Found).product.name)
        }

    @Test
    fun `saving a found product inserts it with its barcode`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "222", name = "Nutella")
            val client = FakeOffProductRepository(mapOf("222" to OffLookupResult.Found(offProduct)))
            val repository = FakeInventoryRepository()
            val viewModel = BarcodeScanViewModel(repository, FakeCategoryRepository(), FakePantryRepository(), client)
            viewModel.onBarcodeDetected("222")

            viewModel.onQuantityTextChanged("3")
            viewModel.onSaveClicked()

            assertTrue(viewModel.uiState.value.isSaved)
            val saved = repository.getAllOnce().single()
            assertEquals("Nutella", saved.name)
            assertEquals(3.0, saved.quantity, 0.0)
            assertEquals("222", saved.barcode)
        }

    @Test
    fun `an unknown barcode allows creating the product manually`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeInventoryRepository()
            val viewModel =
                BarcodeScanViewModel(
                    repository,
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    FakeOffProductRepository(),
                )

            viewModel.onBarcodeDetected("333")
            assertTrue(viewModel.uiState.value.lookup is BarcodeLookupState.NotFound)

            viewModel.onNameChanged("Produit maison")
            viewModel.onQuantityTextChanged("1")
            viewModel.onSaveClicked()

            val saved = repository.getAllOnce().single()
            assertEquals("Produit maison", saved.name)
            assertEquals("333", saved.barcode)
        }

    @Test
    fun `a network error can be retried`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "444", name = "Nutella")
            var shouldFail = true
            val client =
                object : OffProductRepository {
                    override suspend fun lookupProduct(barcode: String): OffLookupResult =
                        if (shouldFail) OffLookupResult.NetworkError else OffLookupResult.Found(offProduct)

                    override suspend fun refreshProduct(barcode: String): OffLookupResult = lookupProduct(barcode)
                }
            val viewModel =
                BarcodeScanViewModel(
                    FakeInventoryRepository(),
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    client,
                )

            viewModel.onBarcodeDetected("444")
            assertTrue(viewModel.uiState.value.lookup is BarcodeLookupState.Error)

            shouldFail = false
            viewModel.onRetryLookup()

            assertTrue(viewModel.uiState.value.lookup is BarcodeLookupState.Found)
        }

    @Test
    fun `a second detection is ignored while a lookup is already resolving or resolved`() =
        runTest(mainDispatcherRule.dispatcher) {
            val client = FakeOffProductRepository()
            val viewModel =
                BarcodeScanViewModel(
                    FakeInventoryRepository(),
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    client,
                )

            viewModel.onBarcodeDetected("111")
            viewModel.onBarcodeDetected("999")

            assertEquals("111", client.lastRequestedBarcode)
        }

    @Test
    fun `cannot save without a name or a positive quantity`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "555", name = "Nutella")
            val client = FakeOffProductRepository(mapOf("555" to OffLookupResult.Found(offProduct)))
            val repository = FakeInventoryRepository()
            val viewModel = BarcodeScanViewModel(repository, FakeCategoryRepository(), FakePantryRepository(), client)
            viewModel.onBarcodeDetected("555")

            viewModel.onNameChanged("")
            assertEquals(false, viewModel.uiState.value.canSave)

            viewModel.onNameChanged("Nutella")
            viewModel.onQuantityTextChanged("0")
            assertEquals(false, viewModel.uiState.value.canSave)

            viewModel.onSaveClicked()
            assertEquals(0, repository.getAllOnce().size)
        }

    @Test
    fun `saving a new product with an expiration date tags every unit with it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val offProduct = OffProduct(barcode = "666", name = "Yaourt")
            val client = FakeOffProductRepository(mapOf("666" to OffLookupResult.Found(offProduct)))
            val repository = FakeInventoryRepository()
            val viewModel = BarcodeScanViewModel(repository, FakeCategoryRepository(), FakePantryRepository(), client)
            viewModel.onBarcodeDetected("666")

            viewModel.onQuantityTextChanged("2")
            viewModel.onExpirationDateChanged(LocalDate.of(2030, 1, 1))
            viewModel.onSaveClicked()

            val saved = repository.getAllOnce().single()
            val expected = LocalDate.of(2030, 1, 1).toEpochMillis()
            assertEquals(listOf(expected, expected), repository.getItems(saved.id).map { it.expirationDate })
        }

    @Test
    fun `adding to an already present product tags only the new unit with the entered date`() =
        runTest(mainDispatcherRule.dispatcher) {
            val existing = productWithBarcode("111")
            val repository = FakeInventoryRepository(listOf(existing))
            val viewModel =
                BarcodeScanViewModel(
                    repository,
                    FakeCategoryRepository(),
                    FakePantryRepository(),
                    FakeOffProductRepository(),
                )
            viewModel.onBarcodeDetected("111")

            val newDate = LocalDate.of(2030, 6, 15)
            viewModel.onExpirationDateChanged(newDate)
            viewModel.onAdjustExistingQuantity(1.0)

            val dates = repository.getItems(existing.id).map { it.expirationDate }
            assertEquals(3, dates.size)
            assertEquals(1, dates.count { it == newDate.toEpochMillis() })
            assertEquals(2, dates.count { it == null })
        }

    private fun productWithBarcode(barcode: String): ProductEntity =
        ProductEntity(id = 1, name = "Eau minérale", quantity = 2.0, unit = QuantityUnit.PIECE.name, barcode = barcode)
}
