package com.rangele.inventory.backup

import androidx.room.withTransaction
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.local.entity.CategoryEntity
import com.rangele.inventory.data.local.entity.HistoryEntryEntity
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.local.entity.ProductItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupRepositoryImpl(
    private val database: AppDatabase,
) : BackupRepository {
    override suspend fun exportTo(output: OutputStream) {
        val products = database.productDao().getAllOnce()
        val categories = database.categoryDao().getAllOnce()
        val history = database.historyEntryDao().getAllOnce()
        val productItems = database.productItemDao().getAllOnce()

        val root =
            JSONObject().apply {
                put("version", BACKUP_FORMAT_VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("products", JSONArray(products.map { it.toJson() }))
                put("categories", JSONArray(categories.map { it.toJson() }))
                put("history", JSONArray(history.map { it.toJson() }))
                put("productItems", JSONArray(productItems.map { it.toJson() }))
            }
        output.write(root.toString().toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    override suspend fun importFrom(input: InputStream): BackupImportResult {
        val root = JSONObject(input.bufferedReader(StandardCharsets.UTF_8).readText())
        val products = root.getJSONArray("products").toEntityList { it.toProductEntity() }
        val categories = root.getJSONArray("categories").toEntityList { it.toCategoryEntity() }
        val history = root.getJSONArray("history").toEntityList { it.toHistoryEntryEntity() }
        // Absent from backups made before per-item expiration dates existed.
        val productItems =
            if (root.has("productItems")) {
                root.getJSONArray("productItems").toEntityList { it.toProductItemEntity() }
            } else {
                emptyList()
            }

        database.withTransaction {
            database.productDao().deleteAll()
            database.categoryDao().deleteAll()
            database.historyEntryDao().deleteAll()
            database.productItemDao().deleteAll()

            database.categoryDao().insertAll(categories)
            database.productDao().insertAll(products)
            database.historyEntryDao().insertAll(history)
            database.productItemDao().insertAll(productItems)
        }

        return BackupImportResult(
            products = products.size,
            categories = categories.size,
            historyEntries = history.size,
        )
    }

    private companion object {
        const val BACKUP_FORMAT_VERSION = 1
    }
}

private fun <T> JSONArray.toEntityList(transform: (JSONObject) -> T): List<T> =
    List(length()) { transform(getJSONObject(it)) }

private fun ProductEntity.toJson(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("name", name)
        put("quantity", quantity)
        put("unit", unit)
        put("updatedAt", updatedAt)
        put("expirationDate", expirationDate ?: JSONObject.NULL)
        put("category", category ?: JSONObject.NULL)
        put("lowStockThreshold", lowStockThreshold ?: JSONObject.NULL)
        put("opened", opened)
        put("barcode", barcode ?: JSONObject.NULL)
    }

private fun JSONObject.toProductEntity(): ProductEntity =
    ProductEntity(
        id = getLong("id"),
        name = getString("name"),
        quantity = getDouble("quantity"),
        unit = getString("unit"),
        updatedAt = getLong("updatedAt"),
        expirationDate = if (isNull("expirationDate")) null else getLong("expirationDate"),
        category = if (isNull("category")) null else getString("category"),
        lowStockThreshold = if (isNull("lowStockThreshold")) null else getDouble("lowStockThreshold"),
        opened = getBoolean("opened"),
        barcode = if (isNull("barcode")) null else getString("barcode"),
    )

private fun CategoryEntity.toJson(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("name", name)
    }

private fun JSONObject.toCategoryEntity(): CategoryEntity =
    CategoryEntity(
        id = getLong("id"),
        name = getString("name"),
    )

private fun ProductItemEntity.toJson(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("productId", productId)
        put("expirationDate", expirationDate ?: JSONObject.NULL)
        put("opened", opened)
    }

private fun JSONObject.toProductItemEntity(): ProductItemEntity =
    ProductItemEntity(
        id = getLong("id"),
        productId = getLong("productId"),
        expirationDate = if (isNull("expirationDate")) null else getLong("expirationDate"),
        // Absent from backups made before per-item opened status existed.
        opened = has("opened") && getBoolean("opened"),
    )

private fun HistoryEntryEntity.toJson(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("productName", productName)
        put("quantityRemoved", quantityRemoved)
        put("unit", unit)
        put("timestamp", timestamp)
    }

private fun JSONObject.toHistoryEntryEntity(): HistoryEntryEntity =
    HistoryEntryEntity(
        id = getLong("id"),
        productName = getString("productName"),
        quantityRemoved = getDouble("quantityRemoved"),
        unit = getString("unit"),
        timestamp = getLong("timestamp"),
    )
