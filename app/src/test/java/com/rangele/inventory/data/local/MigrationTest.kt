package com.rangele.inventory.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs the real Migration against real SQLite (via Robolectric, since there is no androidTest suite here). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    fun `migration from 1 to 2 keeps existing products and adds the new columns`() {
        val dbName = "migration-test"
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO products (id, name, quantity, unit, updated_at) " +
                    "VALUES (1, 'Riz basmati', 2.0, 'PIECE', 1000)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        migratedDb.query("SELECT * FROM products WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Riz basmati", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("expiration_date")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("category")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("low_stock_threshold")))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM history_entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun `migration from 2 to 3 keeps existing products and defaults opened to false`() {
        val dbName = "migration-test-2-3"
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO products (id, name, quantity, unit, updated_at) " +
                    "VALUES (1, 'Riz basmati', 2.0, 'PIECE', 1000)",
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        migratedDb.query("SELECT * FROM products WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Riz basmati", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("opened")))
        }
    }
}
