package com.rangele.inventory.ocr

import com.rangele.inventory.data.model.QuantityUnit
import com.rangele.inventory.util.ProductNameMatcher
import java.util.Locale

/**
 * Turns raw OCR text from a grocery receipt into a list of candidate products.
 *
 * This is a deliberately simple heuristic, not a general receipt parser: it line-splits the
 * OCR output, drops lines that look like totals/metadata rather than products, then strips a
 * trailing price and an optional "N x" quantity multiplier from what's left. The user reviews
 * and edits every line before anything is imported, so occasional misses/false positives here
 * are expected and cheap to fix.
 */
class ReceiptParser {
    fun parse(rawText: String): List<ParsedReceiptLine> =
        rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot(::isNoiseLine)
            .mapNotNull(::parseLine)

    private fun isNoiseLine(line: String): Boolean {
        val normalized = ProductNameMatcher.normalize(line)
        if (normalized.isEmpty()) return true

        // A line with no letters at all (pure numbers/symbols) is never a product name.
        if (line.none { it.isLetter() }) return true

        // Dates (12/03/2026) and times (14:32) are receipt metadata, not products.
        if (DATE_PATTERN.containsMatchIn(line) && line.count { it.isLetter() } <= 2) return true
        if (TIME_PATTERN.matches(line)) return true

        // Mostly-digits lines (phone numbers, postal codes...) with barely any letters are
        // metadata, not a product name.
        val digitTokenCount = normalized.split(" ").count { it.isNotEmpty() && it.all(Char::isDigit) }
        if (digitTokenCount >= 2 && normalized.count { it.isLetter() } <= 3) return true

        val padded = " $normalized "
        return NOISE_KEYWORDS.any { padded.contains(" $it ") }
    }

    private fun parseLine(line: String): ParsedReceiptLine? {
        var remainder = line

        remainder = TRAILING_PRICE_PATTERN.replace(remainder, "").trim()

        var quantity = 1.0
        val qtyMatch = QUANTITY_MULTIPLIER_PATTERN.find(remainder)
        if (qtyMatch != null) {
            val rawQuantity = qtyMatch.groupValues[1].ifEmpty { qtyMatch.groupValues[2] }
            val parsed = rawQuantity.replace(',', '.').toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                quantity = parsed
                remainder = remainder.replaceRange(qtyMatch.range, " ")
            }
        }

        val cleanedName = cleanName(remainder)
        if (cleanedName.length < 2 || cleanedName.none { it.isLetter() }) return null

        return ParsedReceiptLine(
            rawText = line,
            name = cleanedName,
            quantity = quantity,
            unit = QuantityUnit.PIECE,
        )
    }

    private fun cleanName(text: String): String {
        val stripped =
            text
                .replace(LEFTOVER_SYMBOLS_PATTERN, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .trim('*', '-', '.', ',')
                .trim()
        return stripped
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ", transform = ::formatWord)
    }

    /**
     * Title-cases a plain word ("TOMATES" -> "Tomates"). A word starting with a digit is left
     * exactly as the OCR read it ("500g", "1L") instead of guessing a capitalization convention
     * for units, which varies receipt to receipt.
     */
    private fun formatWord(word: String): String =
        if (word.first().isDigit()) {
            word
        } else {
            word.lowercase(Locale.FRENCH).replaceFirstChar { it.titlecase(Locale.FRENCH) }
        }

    private companion object {
        val NOISE_KEYWORDS =
            setOf(
                "total",
                "sous",
                "tva",
                "ht",
                "ttc",
                "especes",
                "cb",
                "carte",
                "bancaire",
                "monnaie",
                "rendu",
                "merci",
                "ticket",
                "caisse",
                "date",
                "siret",
                "tel",
                "solde",
                "eur",
                "visa",
                "paiement",
                "regl",
                "reglement",
                "duree",
                "article",
                "articles",
                "code",
                "tva1",
                "tva2",
                "remise",
                "reduction",
                "a payer",
                "npayer",
                "facture",
                "magasin",
                "bienvenue",
                "ouvert",
            )

        val TRAILING_PRICE_PATTERN = Regex("""(\d{1,4}[.,]\d{2})\s*(€|eur|EUR)?\s*$""")
        val QUANTITY_MULTIPLIER_PATTERN =
            Regex("""(?i)\b(\d+(?:[.,]\d+)?)\s*x\b|\bx\s*(\d+(?:[.,]\d+)?)\b""")
        val LEFTOVER_SYMBOLS_PATTERN = Regex("""[^\p{L}\p{Nd} ]""")
        val DATE_PATTERN = Regex("""\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}""")
        val TIME_PATTERN = Regex("""^\d{1,2}[:h]\d{2}(:\d{2})?$""")
    }
}
