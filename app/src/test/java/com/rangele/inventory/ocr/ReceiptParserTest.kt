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

    private val departmentStoreReceipt =
        """
        PRINTEMPS
        Printemps Haussmann Tél: 0142825000
        *** Ticket   client   ***
        Montants exprimés en euros
        N° Client: 0022065577
        Statut:031 Fid-Silver

        DEC MUG AU PRINTEMPS PARIS                 10,00
        34905553030581
        ALI BOISSON GIMBER                         29,90
        2100023146438
        DEC MUG AU PRINTEMPS PARIS                 10,00
        34905553030598

        Total payé                                 49,90
        Payé en CB                                 49,90

        Montant HT   Taux TVA   Mt TVA   Montant TTC
        41,58         20,00      8,32      49,90

        Magasin 010 - Ilot 2 - Mode -
        Etage 0 -GTPV GTPV1 - TPV 364 -
        Transaction 15204 - Opérateur 84772
        29/05/2021 - 17:46:00

        2 9 0 1 0 0 3 6 4 1 5 2 0 4 7

        Gain de POINTS-SHOPPING : 25
        Cumul total de POINTS-SHOPPING : 53
        Nombre de BR disponibles : 0 BR
        Date d'échéance :

        Merci de votre visite et à très bientôt.
        Conservez ce ticket
        Date limite d'échange le 28/06/2021
        hors alimentaire
        """.trimIndent()

    @Test
    fun `extracts only the real product lines from a department store receipt`() {
        val lines = parser.parse(departmentStoreReceipt)
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.name == "Dec Mug Au Printemps Paris" || it.name == "Ali Boisson Gimber" })
    }

    @Test
    fun `drops store name, client and till metadata lines with no price or quantity`() {
        val names = parser.parse(departmentStoreReceipt).map { it.name.lowercase() }
        assertTrue(names.none { it.contains("printemps") && !it.contains("mug") })
        assertTrue(names.none { it.contains("client") })
        assertTrue(names.none { it.contains("fid") || it.contains("silver") })
        assertTrue(names.none { it.contains("transaction") || it.contains("operateur") })
        assertTrue(names.none { it.contains("points") || it.contains("gain") || it.contains("cumul") })
        assertTrue(names.none { it.contains("hors") || it.contains("alimentaire") })
    }
}
