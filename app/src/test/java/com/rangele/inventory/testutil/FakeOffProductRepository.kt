package com.rangele.inventory.testutil

import com.rangele.inventory.barcode.OffLookupResult
import com.rangele.inventory.barcode.OffProductRepository

/** In-memory stand-in for [OffProductRepository], used to unit test ViewModels without a real cache or network. */
class FakeOffProductRepository(
    resultsByBarcode: Map<String, OffLookupResult> = emptyMap(),
) : OffProductRepository {
    private val resultsByBarcode = resultsByBarcode.toMutableMap()

    var lastRequestedBarcode: String? = null
        private set

    var refreshCallCount: Int = 0
        private set

    /** Changes what the next lookup/refresh for [barcode] returns — e.g. to simulate a changed product. */
    fun setResult(
        barcode: String,
        result: OffLookupResult,
    ) {
        resultsByBarcode[barcode] = result
    }

    override suspend fun lookupProduct(barcode: String): OffLookupResult {
        lastRequestedBarcode = barcode
        return resultsByBarcode[barcode] ?: OffLookupResult.NotFound(barcode)
    }

    override suspend fun refreshProduct(barcode: String): OffLookupResult {
        refreshCallCount++
        lastRequestedBarcode = barcode
        return resultsByBarcode[barcode] ?: OffLookupResult.NotFound(barcode)
    }
}
