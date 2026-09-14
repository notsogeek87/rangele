package com.rangele.inventory.barcode

/**
 * Normalise un code-barres pour servir de clé de cache stable, afin qu'un même produit scanné
 * tantôt en UPC-A (12 chiffres) tantôt en EAN-13 (13 chiffres, cas courant sur les mêmes articles
 * selon l'angle/l'app qui a encodé le symbole) ne crée pas deux entrées distinctes.
 *
 * N'affecte que la clé du cache Open Food Facts : le code-barres stocké sur un produit de
 * l'inventaire ([com.rangele.inventory.data.local.entity.ProductEntity.barcode]) n'est pas modifié.
 */
object BarcodeNormalizer {
    fun normalize(rawBarcode: String): String {
        val trimmed = rawBarcode.trim()
        // UPC-A est un EAN-13 auquel il manque le zéro de tête (règle standard GS1).
        return if (trimmed.length == 12 && trimmed.all { it.isDigit() }) "0$trimmed" else trimmed
    }
}
