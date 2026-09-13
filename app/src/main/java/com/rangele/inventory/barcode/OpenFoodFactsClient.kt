package com.rangele.inventory.barcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface OpenFoodFactsClient {
    /** Looks up [barcode] on Open Food Facts; used only to identify/prefill, never as the inventory's own store. */
    suspend fun lookupProduct(barcode: String): OffLookupResult
}

/**
 * Thin client for the [Open Food Facts read API v2](https://openfoodfacts.github.io/openfoodfacts-server/api/),
 * the currently recommended version. No dependency on Retrofit/OkHttp is pulled in for a single GET endpoint,
 * consistent with this app's hand-rolled-over-framework approach elsewhere
 * (see [AppContainer][com.rangele.inventory.AppContainer]).
 */
class OpenFoodFactsClientImpl : OpenFoodFactsClient {
    override suspend fun lookupProduct(barcode: String): OffLookupResult =
        withContext(Dispatchers.IO) {
            runCatching { parseOffResponse(barcode, fetch(barcode)) }
                .recoverCatching {
                    // Transient blips (DNS hiccup, brief timeout) are common on mobile networks;
                    // one retry after a short pause avoids surfacing an error the user would just retry themselves.
                    delay(RETRY_DELAY_MILLIS)
                    parseOffResponse(barcode, fetch(barcode))
                }.getOrElse { OffLookupResult.NetworkError }
        }

    private fun fetch(barcode: String): String {
        val url = URL("$BASE_URL$barcode.json?fields=$FIELDS")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) {
                throw IOException("Open Food Facts a répondu ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product/"
        const val FIELDS = "code,product_name,brands,quantity,categories,image_front_url,image_url"

        // Open Food Facts asks clients to identify themselves with app name, version and a contact/link;
        // a missing or generic User-Agent is treated as abusive traffic and can be throttled or blocked.
        const val USER_AGENT = "Rangele-Android/1.0 (+https://github.com/notsogeek87/rangele)"
        const val TIMEOUT_MILLIS = 15_000
        const val RETRY_DELAY_MILLIS = 1_500L
    }
}

/** Pure parsing extracted out of [OpenFoodFactsClientImpl] so it can be unit tested without a network stub. */
internal fun parseOffResponse(
    barcode: String,
    responseBody: String,
): OffLookupResult {
    val root = JSONObject(responseBody)
    if (root.optInt("status", 0) != 1) return OffLookupResult.NotFound(barcode)

    val product = root.optJSONObject("product") ?: return OffLookupResult.NotFound(barcode)
    val name = product.optString("product_name").ifBlank { null } ?: return OffLookupResult.NotFound(barcode)

    return OffLookupResult.Found(
        OffProduct(
            barcode = barcode,
            name = name,
            brand =
                product
                    .optString("brands")
                    .ifBlank { null }
                    ?.substringBefore(',')
                    ?.trim(),
            packageFormat = product.optString("quantity").ifBlank { null },
            category =
                product
                    .optString("categories")
                    .ifBlank { null }
                    ?.substringAfterLast(',')
                    ?.trim(),
            imageUrl =
                product.optString("image_front_url").ifBlank { null }
                    ?: product.optString("image_url").ifBlank { null },
        ),
    )
}
