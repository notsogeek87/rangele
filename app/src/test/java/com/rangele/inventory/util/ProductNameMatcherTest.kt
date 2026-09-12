package com.rangele.inventory.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductNameMatcherTest {
    @Test
    fun `normalize strips accents, case and punctuation`() {
        assertEquals("tomate cerise", ProductNameMatcher.normalize("Tomates-Cerises !"))
        assertEquals("cafe", ProductNameMatcher.normalize("Café"))
    }

    @Test
    fun `normalize folds simple plurals`() {
        assertEquals("tomate", ProductNameMatcher.normalize("Tomates"))
        assertEquals("pomme", ProductNameMatcher.normalize("Pommes"))
    }

    @Test
    fun `identical names after normalization are a match`() {
        assertTrue(ProductNameMatcher.isLikelyMatch("Tomates", "tomate"))
        assertTrue(ProductNameMatcher.isLikelyMatch("Café moulu", "CAFE MOULU"))
    }

    @Test
    fun `small typos are still considered a match`() {
        assertTrue(ProductNameMatcher.isLikelyMatch("Yaourt nature", "Yahourt nature"))
    }

    @Test
    fun `unrelated names are not a match`() {
        assertFalse(ProductNameMatcher.isLikelyMatch("Lait demi-ecreme", "Papier toilette"))
    }

    @Test
    fun `findBestMatch returns the closest candidate above threshold`() {
        val candidates = listOf("Papier toilette", "Yaourt nature", "Lait")
        val best = ProductNameMatcher.findBestMatch("Yahourts natures", candidates) { it }
        assertEquals("Yaourt nature", best)
    }

    @Test
    fun `findBestMatch returns null when nothing is close enough`() {
        val candidates = listOf("Papier toilette", "Lait")
        assertNull(ProductNameMatcher.findBestMatch("Chocolat noir", candidates) { it })
    }
}
