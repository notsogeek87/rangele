package com.rangele.inventory.data.model

/**
 * Units a product's quantity can be tracked in. [step] is the increment used by the
 * inventory list's +/- buttons, kept coarse for whole units (pieces, packages) and
 * finer for weights/volumes.
 */
enum class QuantityUnit(
    val label: String,
    val step: Double,
) {
    PIECE("pièce", 1.0),
    PACKAGE("paquet", 1.0),
    GRAM("g", 50.0),
    KILOGRAM("kg", 0.5),
    MILLILITER("mL", 50.0),
    LITER("L", 0.5),
    ;

    companion object {
        fun fromStorageValue(value: String): QuantityUnit = entries.firstOrNull { it.name == value } ?: PIECE
    }
}
