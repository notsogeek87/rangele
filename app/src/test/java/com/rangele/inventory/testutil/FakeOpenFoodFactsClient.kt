package com.rangele.inventory.testutil

import com.rangele.inventory.barcode.OffLookupResult
import com.rangele.inventory.barcode.OpenFoodFactsClient

/** In-memory stand-in for [OpenFoodFactsClient], used to unit test ViewModels without real network calls. */
class FakeOpenFoodFactsClient(
    private val resultsByBarcode: Map<String, OffLookupResult> = emptyMap(),
) : OpenFoodFactsClient {
    var lastRequestedBarcode: String? = null
        private set

    override suspend fun lookupProduct(barcode: String): OffLookupResult {
        lastRequestedBarcode = barcode
        return resultsByBarcode[barcode] ?: OffLookupResult.NotFound(barcode)
    }
}
