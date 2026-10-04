package com.rangele.inventory.barcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
class OpenFoodFactsClientImpl(
    private val baseUrls: List<String> = DEFAULT_BASE_URLS,
    /** Base générale interrogée en dernier recours quand aucune base Open * Facts ne connaît le code ; null = désactivée. */
    private val upcItemDbUrl: String? = UPC_ITEM_DB_URL,
) : OpenFoodFactsClient {
    /**
     * Interroge les bases sœurs dans l'ordre (alimentaire, cosmétique, autres produits) : elles partagent
     * le même format d'API. Le premier résultat trouvé gagne ; un « inconnu » passe à la base suivante.
     * Si aucune base ne connaît le produit mais qu'au moins une était injoignable, on renvoie
     * [OffLookupResult.NetworkError] pour ne pas mettre en cache un faux « introuvable ».
     * Si aucune ne trouve, UPCitemdb est interrogé en dernier recours (voir [lookupOnUpcItemDb]).
     */
    override suspend fun lookupProduct(barcode: String): OffLookupResult =
        withContext(Dispatchers.IO) {
            val openResult = lookupOnOpenBases(barcode)
            if (openResult is OffLookupResult.Found || upcItemDbUrl == null) return@withContext openResult
            when (val fallback = lookupOnUpcItemDb(upcItemDbUrl, barcode)) {
                is OffLookupResult.Found -> fallback
                // Pas de résultat ailleurs : on garde le verdict des bases Open (introuvable ou réseau).
                else -> if (fallback is OffLookupResult.NetworkError) OffLookupResult.NetworkError else openResult
            }
        }

    private suspend fun lookupOnOpenBases(barcode: String): OffLookupResult =
        coroutineScope {
            // Requêtes lancées en parallèle (la latence est celle de la base la plus lente, pas la somme),
            // mais départagées dans l'ordre de priorité de [baseUrls].
            val lookups = baseUrls.map { baseUrl -> async { lookupOn(baseUrl, barcode) } }
            var hadNetworkError = false
            for (lookup in lookups) {
                when (val result = lookup.await()) {
                    is OffLookupResult.Found -> {
                        lookups.forEach { it.cancel() }
                        return@coroutineScope result
                    }
                    is OffLookupResult.NotFound -> Unit
                    OffLookupResult.NetworkError -> hadNetworkError = true
                }
            }
            if (hadNetworkError) OffLookupResult.NetworkError else OffLookupResult.NotFound(barcode)
        }

    /**
     * Dernier recours : [UPCitemdb](https://www.upcitemdb.com/) (offre gratuite ~100 requêtes/jour/IP, d'où un
     * appel seulement quand tout le reste a échoué). Catalogue surtout américain : titres en anglais.
     */
    private suspend fun lookupOnUpcItemDb(
        url: String,
        barcode: String,
    ): OffLookupResult {
        repeat(UPC_MAX_ATTEMPTS) { attempt ->
            val result = runCatching { parseUpcItemDbResponse(barcode, fetch("$url$barcode")) }
            result.getOrNull()?.let { return it }
            if (attempt < UPC_MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MILLIS)
        }
        return OffLookupResult.NetworkError
    }

    private suspend fun lookupOn(
        baseUrl: String,
        barcode: String,
    ): OffLookupResult {
        // Transient blips (DNS hiccup, réseau mobile qui se réveille, 5xx passager d'une base communautaire)
        // sont courants : quelques nouvelles tentatives espacées évitent d'afficher une erreur que
        // l'utilisateur corrigerait lui-même en rescannant.
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { parseOffResponse(barcode, fetch("$baseUrl$barcode.json?fields=$FIELDS")) }
            result.getOrNull()?.let { return it }
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MILLIS * (attempt + 1))
        }
        return OffLookupResult.NetworkError
    }

    private fun fetch(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val code = connection.responseCode
            when {
                code in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                // Les bases sœurs répondent 404 (avec un corps JSON `status: 0`) pour un produit inconnu ou
                // d'un autre type : c'est un « introuvable », pas une panne réseau.
                code == 404 -> connection.errorStream?.bufferedReader()?.use { it.readText() } ?: NOT_FOUND_BODY
                else -> throw IOException("Open Food Facts a répondu $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        val DEFAULT_BASE_URLS =
            listOf(
                "https://world.openfoodfacts.org/api/v2/product/",
                "https://world.openbeautyfacts.org/api/v2/product/",
                "https://world.openproductsfacts.org/api/v2/product/",
            )
        const val UPC_ITEM_DB_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc="
        private const val UPC_MAX_ATTEMPTS = 2
        private const val NOT_FOUND_BODY = "{\"status\":0}"
        private const val FIELDS =
            "code,product_name,brands,quantity,categories,image_front_url,image_url,nutriscore_grade"

        // Open Food Facts asks clients to identify themselves with app name, version and a contact/link;
        // a missing or generic User-Agent is treated as abusive traffic and can be throttled or blocked.
        private const val USER_AGENT = "Rangele-Android/1.0 (+https://github.com/notsogeek87/rangele)"
        private const val TIMEOUT_MILLIS = 15_000
        private const val RETRY_DELAY_MILLIS = 1_500L
        private const val MAX_ATTEMPTS = 3
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
            nutriscore =
                product
                    .optString("nutriscore_grade")
                    .ifBlank { null }
                    ?.lowercase()
                    ?.takeIf { it.length == 1 && it in "abcde" },
        ),
    )
}

/** Titres UPCitemdb finissant par un identifiant Amazon entre parenthèses, ex. « ... 4.2 oz. (B002D48R6A) ». */
private val TRAILING_ASIN = Regex("""\s*\([A-Z0-9]{10}\)\s*$""")

/**
 * Parsing de la réponse UPCitemdb (`items[0]`). La catégorie n'est volontairement pas reprise : elle est en
 * anglais et sous forme de chemin (« Health & Beauty > ... »), et les catégories de l'app sont créées
 * automatiquement à partir de ce champ.
 */
internal fun parseUpcItemDbResponse(
    barcode: String,
    responseBody: String,
): OffLookupResult {
    val item =
        JSONObject(responseBody).optJSONArray("items")?.optJSONObject(0)
            ?: return OffLookupResult.NotFound(barcode)
    val name =
        item
            .optString("title")
            .replace(TRAILING_ASIN, "")
            .trim()
            .ifBlank { null }
            ?: return OffLookupResult.NotFound(barcode)

    return OffLookupResult.Found(
        OffProduct(
            barcode = barcode,
            name = name,
            brand = item.optString("brand").ifBlank { null },
            packageFormat = item.optString("size").ifBlank { null },
            imageUrl = item.optJSONArray("images")?.optString(0)?.ifBlank { null },
            fromUpcItemDb = true,
        ),
    )
}
