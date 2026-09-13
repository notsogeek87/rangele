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
