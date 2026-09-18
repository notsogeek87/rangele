package com.rangele.inventory.data.settings

/**
 * Préférence d'apparence choisie dans Paramètres.
 *
 * [SYSTEM] est la valeur par défaut : l'app suit le thème clair/sombre du système, comme avant
 * l'ajout de ce réglage. [LIGHT] et [DARK] le forcent indépendamment du système.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStorageValue(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
