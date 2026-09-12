package com.rangele.inventory.ocr

import com.rangele.inventory.data.model.QuantityUnit
import java.time.LocalDate
import java.util.UUID

/**
 * One candidate product extracted from a scanned receipt, before the user reviews it.
 * [id] is a local, in-memory identifier only used to key UI rows/edits — it has no relation
 * to any database id.
 */
data class ParsedReceiptLine(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.PIECE,
    val included: Boolean = true,
    /** Id of an existing inventory product this line looks like a match for, if any. */
    val matchedProductId: Long? = null,
    val matchedProductName: String? = null,
    /** Optional, set by the user during review — OCR never infers this. */
    val expirationDate: LocalDate? = null,
)
