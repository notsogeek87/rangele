package com.rangele.inventory.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs under Robolectric so `org.json.JSONObject` is a real implementation rather than the JVM unit test stub. */
@RunWith(RobolectricTestRunner::class)
class OpenFoodFactsClientTest {
    @Test
    fun `a found product is parsed with its main fields`() {
        val body =
            """
            {
              "code": "3017620422003",
              "status": 1,
              "status_verbose": "product found",
              "product": {
                "product_name": "Nutella",
                "brands": "Ferrero,Nutella",
                "quantity": "400 g",
                "categories": "en:Spreads, en:Sweet spreads, en:Hazelnut spreads",
                "image_front_url": "https://images.openfoodfacts.org/front.jpg",
                "image_url": "https://images.openfoodfacts.org/full.jpg",
                "nutriscore_grade": "e"
              }
            }
            """.trimIndent()

        val result = parseOffResponse("3017620422003", body)

        assertTrue(result is OffLookupResult.Found)
        val product = (result as OffLookupResult.Found).product
        assertEquals("3017620422003", product.barcode)
        assertEquals("Nutella", product.name)
        assertEquals("Ferrero", product.brand)
        assertEquals("400 g", product.packageFormat)
        assertEquals("en:Hazelnut spreads", product.category)
        assertEquals("https://images.openfoodfacts.org/front.jpg", product.imageUrl)
        assertEquals("e", product.nutriscore)
    }

    @Test
    fun `an unknown or not-applicable nutriscore grade is dropped`() {
        val body =
            """
            {
              "status": 1,
              "product": {
                "product_name": "Sel de table",
                "nutriscore_grade": "not-applicable"
              }
            }
            """.trimIndent()

        val result = parseOffResponse("123", body) as OffLookupResult.Found

        assertEquals(null, result.product.nutriscore)
    }

    @Test
    fun `falls back to the full-size image when no front image is available`() {
        val body =
            """
            {
              "status": 1,
              "product": {
                "product_name": "Eau minérale",
                "image_url": "https://images.openfoodfacts.org/full.jpg"
              }
            }
            """.trimIndent()

        val result = parseOffResponse("123", body) as OffLookupResult.Found

        assertEquals("https://images.openfoodfacts.org/full.jpg", result.product.imageUrl)
    }

    @Test
    fun `a status of 0 means the product is not known to Open Food Facts`() {
        val body = """{"status": 0, "status_verbose": "product not found"}"""

        val result = parseOffResponse("0000000000000", body)

        assertEquals(OffLookupResult.NotFound("0000000000000"), result)
    }

    @Test
    fun `a product without a name is treated as not found`() {
        val body = """{"status": 1, "product": {"brands": "Some brand"}}"""

        val result = parseOffResponse("123", body)

        assertEquals(OffLookupResult.NotFound("123"), result)
    }
}
