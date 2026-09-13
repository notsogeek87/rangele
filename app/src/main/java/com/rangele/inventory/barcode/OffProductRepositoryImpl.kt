package com.rangele.inventory.barcode

import com.rangele.inventory.data.local.dao.OffProductCacheDao
import com.rangele.inventory.data.local.entity.OffProductCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OffProductRepositoryImpl(
    private val client: OpenFoodFactsClient,
    private val cacheDao: OffProductCacheDao,
    /**
     * Scope sur lequel tourne l'actualisation en arrière-plan d'un résultat périmé : volontairement
     * pas `viewModelScope`, pour que la mise à jour du cache survive si l'utilisateur quitte l'écran
     * de scan avant la fin de la requête.
     */
    private val backgroundScope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : OffProductRepository {
    override suspend fun lookupProduct(barcode: String): OffLookupResult {
        val normalizedBarcode = BarcodeNormalizer.normalize(barcode)
        val cached = cacheDao.getByBarcode(normalizedBarcode)
        val age = cached?.let { now() - it.fetchedAt }

        if (cached != null && cached.found) {
            if (age != null && age <= POSITIVE_TTL_MILLIS) {
                return OffLookupResult.Found(cached.toOffProduct(barcode))
            }
            // Périmé : on sert la donnée existante tout de suite, l'actualisation se fait sans bloquer l'appelant.
            backgroundScope.launch { refreshProduct(barcode) }
            return OffLookupResult.Found(cached.toOffProduct(barcode))
        }

        if (cached != null && !cached.found && age != null && age <= NEGATIVE_TTL_MILLIS) {
            return OffLookupResult.NotFound(barcode)
        }

        return fetchAndCache(barcode, normalizedBarcode)
    }

    override suspend fun refreshProduct(barcode: String): OffLookupResult {
        val normalizedBarcode = BarcodeNormalizer.normalize(barcode)
        return fetchAndCache(barcode, normalizedBarcode)
    }

    /**
     * [barcode] est celui d'origine (avant normalisation), reporté sur le résultat pour que l'appelant
     * voie toujours le code-barres qu'il a demandé, que la réponse vienne du cache ou du réseau.
     */
    private suspend fun fetchAndCache(
        barcode: String,
        normalizedBarcode: String,
    ): OffLookupResult =
        when (val result = client.lookupProduct(normalizedBarcode)) {
            is OffLookupResult.Found -> {
                val product = result.product.copy(barcode = barcode)
                cacheDao.upsert(product.toCacheEntity(normalizedBarcode, now()))
                OffLookupResult.Found(product)
            }
            is OffLookupResult.NotFound -> {
                cacheDao.upsert(negativeCacheEntity(normalizedBarcode, now()))
                OffLookupResult.NotFound(barcode)
            }
            // Une écriture de cache ratée, ou l'API injoignable, ne doivent jamais faire échouer le
            // lookup : l'ancienne donnée en cache (le cas échéant) reste la meilleure réponse disponible.
            OffLookupResult.NetworkError -> OffLookupResult.NetworkError
        }

    private companion object {
        const val POSITIVE_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val NEGATIVE_TTL_MILLIS = 24L * 60 * 60 * 1000
    }
}

private fun OffProductCacheEntity.toOffProduct(requestedBarcode: String) =
    OffProduct(
        barcode = requestedBarcode,
        name = name.orEmpty(),
        brand = brand,
        packageFormat = packageFormat,
        category = category,
        imageUrl = imageUrl,
    )

private fun OffProduct.toCacheEntity(
    normalizedBarcode: String,
    fetchedAt: Long,
) = OffProductCacheEntity(
    barcode = normalizedBarcode,
    found = true,
    name = name,
    brand = brand,
    packageFormat = packageFormat,
    category = category,
    imageUrl = imageUrl,
    fetchedAt = fetchedAt,
)

private fun negativeCacheEntity(
    normalizedBarcode: String,
    fetchedAt: Long,
) = OffProductCacheEntity(
    barcode = normalizedBarcode,
    found = false,
    name = null,
    brand = null,
    packageFormat = null,
    category = null,
    imageUrl = null,
    fetchedAt = fetchedAt,
)
