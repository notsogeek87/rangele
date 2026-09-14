package com.rangele.inventory.barcode

/** Informations récupérées depuis Open Food Facts pour préremplir un produit avant validation. */
data class OffProduct(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    /** Format du colisage renseigné sur Open Food Facts (ex. "500 g", "1 L") — pas une quantité en stock. */
    val packageFormat: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    /** Grade Nutri-Score ("a" à "e" en minuscule), null si non renseigné sur Open Food Facts. */
    val nutriscore: String? = null,
)

/** Résultat d'une recherche de produit par code-barres sur Open Food Facts. */
sealed interface OffLookupResult {
    data class Found(
        val product: OffProduct,
    ) : OffLookupResult

    data class NotFound(
        val barcode: String,
    ) : OffLookupResult

    data object NetworkError : OffLookupResult
}
