package com.rangele.inventory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rangele.inventory.data.model.QuantityUnit

@Entity(tableName = "products", indices = [Index("barcode")])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * Date d'ajout du produit à l'inventaire (epoch millis), figée à la création — là où
     * [updatedAt] est repoussé par chaque ajustement de quantité. C'est elle qui porte le tri
     * « ajout récent », tri par défaut de l'inventaire : un produit réapprovisionné ne doit pas
     * remonter en tête de liste comme s'il venait d'entrer dans le placard.
     *
     * Défaut SQL déclaré pour que la colonne ajoutée par migration corresponde à celle que Room
     * crée à l'installation (même raison que [opened]).
     */
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    /** Epoch millis (UTC midnight of the expiration day), null when not tracked. */
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Long? = null,
    /** Free-form category name (see [com.rangele.inventory.data.local.entity.CategoryEntity]), null when unassigned. */
    val category: String? = null,
    /** Below this quantity the product is surfaced in the suggested shopping list; null disables it. */
    @ColumnInfo(name = "low_stock_threshold")
    val lowStockThreshold: Double? = null,
    /**
     * Whether the product has already been opened/started.
     *
     * The SQL default is declared so a migrated column (`ALTER TABLE ... ADD COLUMN ... NOT NULL
     * DEFAULT 0`, which SQLite requires) matches the table Room creates on a fresh install.
     */
    @ColumnInfo(defaultValue = "0")
    val opened: Boolean = false,
    /**
     * Ajout manuel à la liste de courses suggérée, indépendamment de [lowStockThreshold] : le
     * produit y figure tant que ce drapeau est levé, même avec du stock. C'est le pendant manuel
     * de la suggestion automatique par seuil, et il ne se baisse que par une action de
     * l'utilisateur (réapprovisionner ne le retire pas).
     *
     * Défaut SQL déclaré pour que la colonne ajoutée par migration corresponde à celle que Room
     * crée à l'installation (même raison que [opened]).
     */
    @ColumnInfo(name = "in_shopping_list", defaultValue = "0")
    val inShoppingList: Boolean = false,
    /** Code-barres scanné (EAN/UPC), utilisé pour reconnaître le produit lors d'un futur scan. */
    val barcode: String? = null,
    /** Placard du produit (voir [com.rangele.inventory.data.local.entity.PantryEntity]), null si non assigné. */
    @ColumnInfo(name = "pantry_id")
    val pantryId: Long? = null,
    /** Grade Nutri-Score ("a" à "e"), renseigné depuis Open Food Facts lors d'un scan de code-barres. */
    val nutriscore: String? = null,
) {
    val quantityUnit: QuantityUnit
        get() = QuantityUnit.fromStorageValue(unit)
}
