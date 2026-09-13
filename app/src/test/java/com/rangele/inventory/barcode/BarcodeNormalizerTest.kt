package com.rangele.inventory.barcode

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeNormalizerTest {
    @Test
    fun `a UPC-A code is prefixed with a zero to match its EAN-13 form`() {
        assertEquals("0012345678905", BarcodeNormalizer.normalize("012345678905"))
    }

    @Test
    fun `an EAN-13 code is left unchanged`() {
        assertEquals("3017620422003", BarcodeNormalizer.normalize("3017620422003"))
    }

    @Test
    fun `an EAN-8 code is left unchanged`() {
        assertEquals("96385074", BarcodeNormalizer.normalize("96385074"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("3017620422003", BarcodeNormalizer.normalize(" 3017620422003 "))
    }

    @Test
    fun `a non-numeric code is left unchanged`() {
        assertEquals("CODE128-ABC", BarcodeNormalizer.normalize("CODE128-ABC"))
    }
}
