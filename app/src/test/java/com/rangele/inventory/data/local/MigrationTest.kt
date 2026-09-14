package com.rangele.inventory.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Creates a real v1 SQLite database, then lets Room apply the migrations up to the current version.
 * Room validates the resulting schema against the compiled entities while opening, so a migration
 * that forgets a column, a table or an index fails this test.
 *
 * Room's own MigrationTestHelper is deliberately not used: it loads the exported schema JSON of the
 * *older* versions, but only the current version is generated at build time and no schema is checked
 * into the repository, so it can only ever throw FileNotFoundException here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `a version 1 database migrates to the current version without losing data`() =
        runTest {
            createVersion1Database()

            val database =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, databaseName)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                    ).allowMainThreadQueries()
                    .build()

            try {
                // Opening the database runs all migrations and makes Room validate the final schema.
                val products = database.productDao().getAllOnce()

                assertEquals(1, products.size)
                val product = products.first()
                assertEquals("Riz basmati", product.name)
                assertEquals(2.0, product.quantity, 0.0)
                assertNull(product.expirationDate)
                assertNull(product.category)
                assertNull(product.lowStockThreshold)
                assertFalse(product.opened)
                assertFalse(product.inShoppingList)
                assertNull(product.barcode)
                assertNull(product.pantryId)
                assertNull(product.nutriscore)

                // Unité discrète sans date ni statut "entamé" : deux articles sont créés sans rien à reporter.
                val items = database.productItemDao().getForProduct(product.id)
                assertEquals(2, items.size)
                assertTrue(items.all { it.expirationDate == null })
                assertTrue(items.all { !it.opened })

                assertTrue(database.historyEntryDao().getAllOnce().isEmpty())
                assertTrue(database.pantryDao().getAllOnce().isEmpty())
                assertNull(database.offProductCacheDao().getByBarcode("0000000000000"))
            } finally {
                database.close()
            }
        }

    @Test
    fun `a discrete product with an expiration date backfills that date onto each of its items`() =
        runTest {
            createVersion5Database(expirationDate = 5000L)

            val database =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, databaseName)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                    ).allowMainThreadQueries()
                    .build()

            try {
                val product = database.productDao().getAllOnce().first()
                assertEquals(5000L, product.expirationDate)

                val items = database.productItemDao().getForProduct(product.id)
                assertEquals(2, items.size)
                assertTrue(items.all { it.expirationDate == 5000L })
            } finally {
                database.close()
            }
        }

    @Test
    fun `an opened discrete product backfills the opened status onto each of its items`() =
        runTest {
            createVersion6Database(productOpened = true)

            val database =
                Room
                    .databaseBuilder(context, AppDatabase::class.java, databaseName)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .allowMainThreadQueries()
                    .build()

            try {
                val product = database.productDao().getAllOnce().first()
                assertTrue(product.opened)

                val items = database.productItemDao().getForProduct(product.id)
                assertEquals(2, items.size)
                assertTrue(items.all { it.opened })
            } finally {
                database.close()
            }
        }

    /** The products table as Room created it in version 1, before expiration dates and categories. */
    private fun createVersion1Database() {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `products` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`unit` TEXT NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL)",
            )
            database.execSQL(
                "INSERT INTO products (id, name, quantity, unit, updated_at) " +
                    "VALUES (1, 'Riz basmati', 2.0, 'PIECE', 1000)",
            )
            database.version = 1
        }
    }

    /** The full schema as of version 5 (just before per-item dates), with one discrete-unit product. */
    private fun createVersion5Database(expirationDate: Long?) {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `products` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`unit` TEXT NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, " +
                    "`expiration_date` INTEGER, " +
                    "`category` TEXT, " +
                    "`low_stock_threshold` REAL, " +
                    "`opened` INTEGER NOT NULL DEFAULT 0, " +
                    "`barcode` TEXT, " +
                    "`pantry_id` INTEGER)",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
            database.execSQL(
                "INSERT INTO products (id, name, quantity, unit, updated_at, expiration_date) " +
                    "VALUES (1, 'Riz basmati', 2.0, 'PIECE', 1000, ${expirationDate ?: "NULL"})",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `history_entries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_name` TEXT NOT NULL, " +
                    "`quantity_removed` REAL NOT NULL, `unit` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `pantries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                    "`is_default` INTEGER NOT NULL DEFAULT 0)",
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pantries_name` ON `pantries` (`name`)")
            database.version = 5
        }
    }

    /** The full schema as of version 6 (just before per-item opened status), with one discrete-unit product. */
    private fun createVersion6Database(productOpened: Boolean) {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `products` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`quantity` REAL NOT NULL, " +
                    "`unit` TEXT NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, " +
                    "`expiration_date` INTEGER, " +
                    "`category` TEXT, " +
                    "`low_stock_threshold` REAL, " +
                    "`opened` INTEGER NOT NULL DEFAULT 0, " +
                    "`barcode` TEXT, " +
                    "`pantry_id` INTEGER)",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
            database.execSQL(
                "INSERT INTO products (id, name, quantity, unit, updated_at, opened) " +
                    "VALUES (1, 'Riz basmati', 2.0, 'PIECE', 1000, ${if (productOpened) 1 else 0})",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `history_entries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_name` TEXT NOT NULL, " +
                    "`quantity_removed` REAL NOT NULL, `unit` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `pantries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                    "`is_default` INTEGER NOT NULL DEFAULT 0)",
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pantries_name` ON `pantries` (`name`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `product_items` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`product_id` INTEGER NOT NULL, " +
                    "`expiration_date` INTEGER)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_product_items_product_id` ON `product_items` (`product_id`)",
            )
            database.execSQL("INSERT INTO product_items (product_id, expiration_date) VALUES (1, NULL)")
            database.execSQL("INSERT INTO product_items (product_id, expiration_date) VALUES (1, NULL)")
            database.version = 6
        }
    }
}
