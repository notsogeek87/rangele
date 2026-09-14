package com.rangele.inventory.barcode

/**
 * Point d'entrée unique pour résoudre un code-barres en produit Open Food Facts, orchestrant le
 * cache local ([com.rangele.inventory.data.local.entity.OffProductCacheEntity]) et le client
 * réseau ([OpenFoodFactsClient]). Voir [OffProductRepositoryImpl] pour la politique de fraîcheur.
 */
interface OffProductRepository {
    /**
     * Recherche cache-first : instantané si un résultat valide est en cache, sinon interroge Open
     * Food Facts. Un résultat positif périmé (> 30 jours) est retourné immédiatement tel quel,
     * pendant qu'une actualisation est tentée en arrière-plan sans bloquer l'appelant.
     */
    suspend fun lookupProduct(barcode: String): OffLookupResult

    /** Ignore le cache et interroge directement Open Food Facts, mettant à jour le cache avec le résultat. */
    suspend fun refreshProduct(barcode: String): OffLookupResult
}
