package com.rangele.inventory.testutil

import com.rangele.inventory.barcode.OffLookupResult
import com.rangele.inventory.barcode.OpenFoodFactsClient

/** In-memory stand-in for [OpenFoodFactsClient], used to unit test ViewModels/repositories without real network calls. */
class FakeOpenFoodFactsClient(
    private val resultsByBarcode: Map<String, OffLookupResult> = emptyMap(),
) : OpenFoodFactsClient {
    var lastRequestedBarcode: String? = null
        private set

    /** Number of times [lookupProduct] was called — lets cache tests assert no network call happened. */
    var requestCount: Int = 0
        private set

    override suspend fun lookupProduct(barcode: String): OffLookupResult {
        lastRequestedBarcode = barcode
        requestCount++
        return resultsByBarcode[barcode] ?: OffLookupResult.NotFound(barcode)
    }
}
