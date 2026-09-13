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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .allowMainThreadQueries()
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
                assertNull(product.barcode)

                assertTrue(database.historyEntryDao().getAllOnce().isEmpty())
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
}
