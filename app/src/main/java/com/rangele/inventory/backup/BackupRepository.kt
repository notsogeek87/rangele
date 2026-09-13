package com.rangele.inventory.backup

import java.io.InputStream
import java.io.OutputStream

/** Counts restored from a backup file, shown to the user after a successful import. */
data class BackupImportResult(
    val products: Int,
    val categories: Int,
    val historyEntries: Int,
)

/**
 * Exports/imports the whole local inventory (products, categories, history) as a single JSON
 * file, so the user can save it wherever they like — including Google Drive, via the system
 * "Save to..." file picker (Storage Access Framework) rather than a custom Drive integration.
 */
interface BackupRepository {
    suspend fun exportTo(output: OutputStream)

    /** Replaces the whole current inventory with the content of [input]. */
    suspend fun importFrom(input: InputStream): BackupImportResult
}
