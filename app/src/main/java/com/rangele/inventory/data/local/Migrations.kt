package com.rangele.inventory.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** V3: expiration dates, categories, low-stock threshold, plus the new tables — no destructive fallback. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN expiration_date INTEGER")
            db.execSQL("ALTER TABLE products ADD COLUMN category TEXT")
            db.execSQL("ALTER TABLE products ADD COLUMN low_stock_threshold REAL")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories (name)",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS history_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "product_name TEXT NOT NULL, " +
                    "quantity_removed REAL NOT NULL, " +
                    "unit TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL)",
            )
        }
    }

/** Ajoute le statut "entamé" d'un produit, indépendant de sa date de péremption. */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN opened INTEGER NOT NULL DEFAULT 0")
        }
    }

/** Ajoute le code-barres scanné, utilisé pour reconnaître un produit déjà présent lors d'un futur scan. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
        }
    }

/** Ajoute les placards (gérés dans les Paramètres) et le placard assigné à chaque produit. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS pantries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "is_default INTEGER NOT NULL DEFAULT 0)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_pantries_name ON pantries (name)",
            )
            db.execSQL("ALTER TABLE products ADD COLUMN pantry_id INTEGER")
        }
    }

/**
 * Passe la date de péremption d'un produit en unité discrète (pièce/paquet) à une date par
 * article : chaque article existant devient une ligne `product_items`, en reportant la date de
 * péremption du produit (si elle existait) sur chacune. `products.expiration_date` est conservé
 * comme cache de la date la plus proche parmi ses articles (voir InventoryRepositoryImpl), donc
 * inchangé par cette migration. Les produits en unité continue (poids/volume) n'ont pas d'article :
 * aucune ligne n'est créée pour eux.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS product_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "product_id INTEGER NOT NULL, " +
                    "expiration_date INTEGER)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_product_items_product_id ON product_items (product_id)",
            )

            db
                .query(
                    "SELECT id, quantity, expiration_date FROM products WHERE unit IN ('PIECE', 'PACKAGE')",
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val productId = cursor.getLong(0)
                        val expirationDate = if (cursor.isNull(2)) null else cursor.getLong(2)
                        val itemCount = Math.round(cursor.getDouble(1)).toInt().coerceAtLeast(0)
                        repeat(itemCount) {
                            db.execSQL(
                                "INSERT INTO product_items (product_id, expiration_date) VALUES (?, ?)",
                                arrayOf<Any?>(productId, expirationDate),
                            )
                        }
                    }
                }
        }
    }
