package com.rangele.inventory.barcode

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rangele.inventory.data.local.AppDatabase
import com.rangele.inventory.data.local.dao.OffProductCacheDao
import com.rangele.inventory.data.local.entity.OffProductCacheEntity
import com.rangele.inventory.testutil.FakeOpenFoodFactsClient
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor

/** Uses a real in-memory Room database (via Robolectric) so the cache's SQL — not a fake — is under test. */
@RunWith(RobolectricTestRunner::class)
class OffProductRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var cacheDao: OffProductCacheDao
    private var currentTime = BASE_TIME

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                // Room exécute ses requêtes suspend sur ses propres threads : advanceUntilIdle() rend la main
                // alors qu'une écriture lancée en arrière-plan est encore en vol, et la lecture qui suit peut
                // voir l'ancienne donnée (c'est ce qui faisait échouer le test d'actualisation en CI).
                // Avec un exécuteur synchrone, tout le travail Room reste dans la coroutine appelante et
                // advanceUntilIdle() attend donc réellement la fin de l'actualisation.
                .setQueryExecutor(DIRECT_EXECUTOR)
                .setTransactionExecutor(DIRECT_EXECUTOR)
                .allowMainThreadQueries()
                .build()
        cacheDao = database.offProductCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `a product present in the cache does not trigger a network request`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = true, fetchedAt = currentTime))
            val client = FakeOpenFoodFactsClient()
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)

            assertEquals(0, client.requestCount)
            assertTrue(result is OffLookupResult.Found)
            assertEquals("Nutella", (result as OffLookupResult.Found).product.name)
        }

    @Test
    fun `a product absent from the cache triggers an Open Food Facts request`() =
        runTest {
            val client = FakeOpenFoodFactsClient(mapOf(BARCODE to OffLookupResult.Found(offProduct())))
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            repository.lookupProduct(BARCODE)

            assertEquals(1, client.requestCount)
        }

    @Test
    fun `a product fetched from Open Food Facts is added to the cache`() =
        runTest {
            val client = FakeOpenFoodFactsClient(mapOf(BARCODE to OffLookupResult.Found(offProduct())))
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            repository.lookupProduct(BARCODE)

            val cached = cacheDao.getByBarcode(BARCODE)
            assertTrue(cached != null && cached.found)
            assertEquals("Nutella", cached?.name)
            assertEquals("b", cached?.nutriscore)
            assertEquals(currentTime, cached?.fetchedAt)
        }

    @Test
    fun `a product cached less than 30 days ago is used directly`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = true, fetchedAt = currentTime - TEN_DAYS_MILLIS))
            val client = FakeOpenFoodFactsClient()
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)

            assertEquals(0, client.requestCount)
            assertTrue(result is OffLookupResult.Found)
        }

    @Test
    fun `a product cached more than 30 days ago is shown immediately then refreshed in the background`() =
        runTest {
            cacheDao.upsert(
                cachedEntry(
                    found = true,
                    name = "Nutella (ancien)",
                    fetchedAt =
                        currentTime - THIRTY_ONE_DAYS_MILLIS,
                ),
            )
            val client =
                FakeOpenFoodFactsClient(
                    mapOf(
                        BARCODE to OffLookupResult.Found(offProduct(name = "Nutella (nouveau)")),
                    ),
                )
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)

            // Résultat immédiat = l'ancienne donnée, sans attendre le réseau.
            assertTrue(result is OffLookupResult.Found)
            assertEquals("Nutella (ancien)", (result as OffLookupResult.Found).product.name)
            assertEquals(0, client.requestCount)

            advanceUntilIdle()

            assertEquals(1, client.requestCount)
            assertEquals("Nutella (nouveau)", cacheDao.getByBarcode(BARCODE)?.name)
        }

    @Test
    fun `an unknown barcode is remembered for 24 hours`() =
        runTest {
            val client = FakeOpenFoodFactsClient(mapOf(BARCODE to OffLookupResult.NotFound(BARCODE)))
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })
            repository.lookupProduct(BARCODE)
            assertEquals(1, client.requestCount)

            currentTime += TWENTY_HOURS_MILLIS
            val result = repository.lookupProduct(BARCODE)

            assertEquals(1, client.requestCount)
            assertTrue(result is OffLookupResult.NotFound)
        }

    @Test
    fun `an unknown barcode can be searched again after the negative cache expires`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = false, fetchedAt = currentTime - TWENTY_FIVE_HOURS_MILLIS))
            val client = FakeOpenFoodFactsClient(mapOf(BARCODE to OffLookupResult.Found(offProduct())))
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)

            assertEquals(1, client.requestCount)
            assertTrue(result is OffLookupResult.Found)
        }

    @Test
    fun `offline with nothing cached surfaces a network error`() =
        runTest {
            val client = FakeOpenFoodFactsClient()
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct("999")

            assertEquals(OffLookupResult.NetworkError, result)
            assertNull(cacheDao.getByBarcode("999"))
        }

    @Test
    fun `offline still serves a cached product`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = true, fetchedAt = currentTime - TEN_DAYS_MILLIS))
            // Le client échouerait s'il était appelé : un produit encore frais ne doit jamais le solliciter.
            val client = FakeOpenFoodFactsClient()
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)

            assertTrue(result is OffLookupResult.Found)
            assertEquals(0, client.requestCount)
        }

    @Test
    fun `a network error during background refresh keeps the previous cached data`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = true, fetchedAt = currentTime - THIRTY_ONE_DAYS_MILLIS))
            val failingClient =
                object : OpenFoodFactsClient {
                    override suspend fun lookupProduct(barcode: String): OffLookupResult = OffLookupResult.NetworkError
                }
            val repository = OffProductRepositoryImpl(failingClient, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.lookupProduct(BARCODE)
            advanceUntilIdle()

            assertTrue(result is OffLookupResult.Found)
            val cached = cacheDao.getByBarcode(BARCODE)
            assertTrue(cached != null && cached.found)
            assertEquals("Nutella", cached?.name)
            assertEquals(currentTime - THIRTY_ONE_DAYS_MILLIS, cached?.fetchedAt)
        }

    @Test
    fun `force refresh ignores a still-fresh cache and hits the network`() =
        runTest {
            cacheDao.upsert(cachedEntry(found = true, name = "Nutella (ancien)", fetchedAt = currentTime))
            val client =
                FakeOpenFoodFactsClient(
                    mapOf(
                        BARCODE to OffLookupResult.Found(offProduct(name = "Nutella (nouveau)")),
                    ),
                )
            val repository = OffProductRepositoryImpl(client, cacheDao, backgroundScope, now = { currentTime })

            val result = repository.refreshProduct(BARCODE)

            assertEquals(1, client.requestCount)
            assertTrue(result is OffLookupResult.Found)
            assertEquals("Nutella (nouveau)", (result as OffLookupResult.Found).product.name)
            assertEquals("Nutella (nouveau)", cacheDao.getByBarcode(BARCODE)?.name)
        }

    private fun offProduct(name: String = "Nutella") =
        OffProduct(barcode = BARCODE, name = name, brand = "Ferrero", nutriscore = "b")

    private fun cachedEntry(
        found: Boolean,
        name: String = "Nutella",
        fetchedAt: Long,
    ) = OffProductCacheEntity(
        barcode = BARCODE,
        found = found,
        name = if (found) name else null,
        brand = if (found) "Ferrero" else null,
        packageFormat = null,
        category = null,
        imageUrl = null,
        nutriscore = if (found) "b" else null,
        fetchedAt = fetchedAt,
    )

    private companion object {
        val DIRECT_EXECUTOR = Executor { it.run() }
        const val BARCODE = "3017620422003"
        const val BASE_TIME = 1_700_000_000_000L
        const val TEN_DAYS_MILLIS = 10L * 24 * 60 * 60 * 1000
        const val THIRTY_ONE_DAYS_MILLIS = 31L * 24 * 60 * 60 * 1000
        const val TWENTY_HOURS_MILLIS = 20L * 60 * 60 * 1000
        const val TWENTY_FIVE_HOURS_MILLIS = 25L * 60 * 60 * 1000
    }
}
