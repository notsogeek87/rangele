package com.rangele.inventory.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    private val parser = ReceiptParser()

    private val sampleReceipt =
        """
        Ticket de caisse
        12/03/2026 14:32
        Tomates cerises 500g          2,50
        2 X Yaourt nature              3,00
        Pain complet x2                 1,80
        Lait demi ecreme 1L             1,10
        3x Pomme golden                 2,70
        SOUS TOTAL                       9,25
        TVA 5.5                          0,51
        TOTAL TTC                       11,10
        CB                              11,10
        MERCI DE VOTRE VISITE
        """.trimIndent()

    @Test
    fun `extracts the five product lines from a sample receipt`() {
        val lines = parser.parse(sampleReceipt)
        assertEquals(5, lines.size)
    }

    @Test
    fun `strips trailing price and keeps default quantity of one when no multiplier`() {
        val line = parser.parse(sampleReceipt).first { it.name.contains("Tomates") }
        assertEquals("Tomates Cerises 500g", line.name)
        assertEquals(1.0, line.quantity, 0.0)
    }

    @Test
    fun `detects a leading N x quantity multiplier`() {
        val line = parser.parse(sampleReceipt).first { it.name.contains("Yaourt") }
        assertEquals("Yaourt Nature", line.name)
        assertEquals(2.0, line.quantity, 0.0)
    }

    @Test
    fun `detects a trailing xN quantity multiplier`() {
        val line = parser.parse(sampleReceipt).first { it.name.contains("Pain") }
        assertEquals("Pain Complet", line.name)
        assertEquals(2.0, line.quantity, 0.0)
    }

    @Test
    fun `detects a compact NxProduct quantity multiplier`() {
        val line = parser.parse(sampleReceipt).first { it.name.contains("Pomme") }
        assertEquals("Pomme Golden", line.name)
        assertEquals(3.0, line.quantity, 0.0)
    }

    @Test
    fun `does not mistake an embedded weight for a quantity multiplier`() {
        val line = parser.parse(sampleReceipt).first { it.name.contains("Lait") }
        assertEquals("Lait Demi Ecreme 1L", line.name)
        assertEquals(1.0, line.quantity, 0.0)
    }

    @Test
    fun `filters out totals, taxes, payment and footer lines`() {
        val names = parser.parse(sampleReceipt).map { it.name.lowercase() }
        assertTrue(names.none { it.contains("total") })
        assertTrue(names.none { it.contains("tva") })
        assertTrue(names.none { it.contains("merci") })
        assertTrue(names.none { it.contains("caisse") })
    }

    @Test
    fun `filters out a pure date-time line`() {
        val names = parser.parse(sampleReceipt).map { it.rawText }
        assertTrue(names.none { it.contains("12/03/2026") })
    }

    @Test
    fun `filters out phone-number-like lines with almost no letters`() {
        val lines = parser.parse("TEL 01 23 45 67 89\nRiz basmati 1kg    2,00")
        assertEquals(1, lines.size)
        assertEquals("Riz Basmati 1kg", lines.first().name)
    }

    @Test
    fun `returns an empty list for a receipt with no product lines`() {
        val lines = parser.parse("MERCI DE VOTRE VISITE\nTOTAL TTC 0,00")
        assertTrue(lines.isEmpty())
    }

    @Test
    fun `ignores blank input`() {
        assertNull(parser.parse("").firstOrNull())
    }
}
