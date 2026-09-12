package com.rangele.inventory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Rounded corner tokens from the Kawaii Pastel Pop design (sm/md/lg = 20/40/60dp).
val ShapeSmall = RoundedCornerShape(20.dp)
val ShapeMedium = RoundedCornerShape(40.dp)
val ShapeLarge = RoundedCornerShape(60.dp)

val KawaiiShapes =
    Shapes(
        extraSmall = ShapeSmall,
        small = ShapeSmall,
        medium = ShapeMedium,
        large = ShapeMedium,
        extraLarge = ShapeLarge,
    )
