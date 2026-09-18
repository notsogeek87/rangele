package com.rangele.inventory.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Grille d'espacement en pas de 4dp. Les écrans piochaient jusqu'ici des valeurs au cas par cas
 * (4, 6, 10, 12, 16, 32dp), ce qui donnait des rythmes verticaux différents d'un écran à l'autre ;
 * ces jetons fixent un vocabulaire commun.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Tailles récurrentes partagées entre écrans. */
object Sizes {
    /** Cible tactile minimale recommandée par Android : toute zone cliquable doit l'atteindre. */
    val touchTarget = 48.dp

    /** Pastille d'initiale d'un produit ([com.rangele.inventory.ui.components.ProductAvatar]). */
    val avatar = 44.dp
}
