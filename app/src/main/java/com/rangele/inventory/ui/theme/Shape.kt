package com.rangele.inventory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Échelle d'arrondis « Pastel Modern ». L'ancienne échelle (20/40/60dp appliquée à tout) rendait
// les cartes de liste bombées et rognait le contenu dans les angles des grandes surfaces. Ici les
// arrondis progressent avec la taille de l'élément, et la forme pilule est réservée aux éléments
// qui la portent vraiment (chips, steppers, FAB).
val ShapeExtraSmall = RoundedCornerShape(10.dp)
val ShapeSmall = RoundedCornerShape(14.dp)
val ShapeMedium = RoundedCornerShape(20.dp)
val ShapeLarge = RoundedCornerShape(26.dp)
val ShapeExtraLarge = RoundedCornerShape(32.dp)

/** Pilule : chips de filtre, stepper de quantité, badges, boutons d'action flottants. */
val ShapePill = RoundedCornerShape(percent = 50)

val RangeleShapes =
    Shapes(
        extraSmall = ShapeExtraSmall,
        small = ShapeSmall,
        medium = ShapeMedium,
        large = ShapeLarge,
        extraLarge = ShapeExtraLarge,
    )
