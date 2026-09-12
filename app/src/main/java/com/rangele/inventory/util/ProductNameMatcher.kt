package com.rangele.inventory.util

import java.text.Normalizer
import kotlin.math.max

/**
 * Heuristic used to decide whether a newly entered/scanned product name is "the same
 * product" as one already in the inventory, so we can offer to merge quantities instead
 * of creating a duplicate row. Deliberately simple: normalize, then compare exactly or by
 * edit-distance ratio. Good enough for a personal pantry list, not meant to be perfect.
 */
object ProductNameMatcher {
    /** Similarity ratio (0..1) at or above which two normalized names are considered a match. */
    private const val SIMILARITY_THRESHOLD = 0.82

    fun normalize(name: String): String {
        val withoutAccents =
            Normalizer
                .normalize(name, Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
        val cleaned =
            withoutAccents
                .lowercase()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        // Very naive singular/plural folding ("tomates" -> "tomate").
        return cleaned
            .split(" ")
            .joinToString(" ") { word -> if (word.length > 3 && word.endsWith("s")) word.dropLast(1) else word }
    }

    fun similarity(
        a: String,
        b: String,
    ): Double {
        val normalizedA = normalize(a)
        val normalizedB = normalize(b)
        if (normalizedA.isEmpty() || normalizedB.isEmpty()) return 0.0
        if (normalizedA == normalizedB) return 1.0
        val distance = levenshtein(normalizedA, normalizedB)
        val longest = max(normalizedA.length, normalizedB.length)
        return 1.0 - (distance.toDouble() / longest)
    }

    fun isLikelyMatch(
        a: String,
        b: String,
    ): Boolean = similarity(a, b) >= SIMILARITY_THRESHOLD

    /** Returns the item from [candidates] most likely to be the same product as [name], if any. */
    fun <T> findBestMatch(
        name: String,
        candidates: List<T>,
        nameOf: (T) -> String,
    ): T? =
        candidates
            .map { it to similarity(name, nameOf(it)) }
            .filter { (_, score) -> score >= SIMILARITY_THRESHOLD }
            .maxByOrNull { (_, score) -> score }
            ?.first

    private fun levenshtein(
        a: String,
        b: String,
    ): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] =
                    minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost,
                    )
            }
        }
        return dp[a.length][b.length]
    }
}
