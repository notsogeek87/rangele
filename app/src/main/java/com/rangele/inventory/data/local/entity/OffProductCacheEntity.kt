package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache local d'un produit Open Food Facts, indexé par code-barres normalisé (voir
 * [com.rangele.inventory.barcode.BarcodeNormalizer]). Une seule ligne par code-barres, qu'un
 * produit ait été trouvé ([found] = true, champs renseignés) ou pas ([found] = false, cache
 * "négatif" pour éviter de reredemander un code-barres inconnu à chaque scan) : voir
 * [com.rangele.inventory.barcode.OffProductRepositoryImpl] pour la logique d'expiration.
 */
@Entity(tableName = "off_product_cache")
data class OffProductCacheEntity(
    @PrimaryKey
    val barcode: String,
    val found: Boolean,
    val name: String?,
    val brand: String?,
    @ColumnInfo(name = "package_format")
    val packageFormat: String?,
    val category: String?,
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    /** Epoch millis de la dernière réponse (succès ou "non trouvé") d'Open Food Facts pour ce code-barres. */
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,
)
