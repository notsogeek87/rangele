package com.rangele.inventory.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// Palette « Pastel Modern » — même famille de teintes que le Kawaii Pastel Pop d'origine (rose,
// violet, bleu ciel, crème, menthe), mais réparties autrement :
//
//  * les pastels saturés ne servent plus de fond d'écran ni de fond de carte : ils deviennent des
//    *conteneurs* (pastilles, chips, badges), c'est là qu'ils se lisent le mieux ;
//  * le canevas devient un neutre très clair à sous-ton rosé, pour que le contenu porte la couleur
//    plutôt que l'inverse ;
//  * chaque teinte a une version *foncée* (même hue, luminosité plus basse) utilisée pour le texte
//    et les éléments interactifs, afin de passer le contraste WCAG AA — l'ancienne paire
//    `primary = #9D84B6` / `onPrimary = #B3E5FC` plafonnait à ~1,4:1, illisible sur un bouton.
//
// Les ratios de contraste indiqués sont calculés sur la surface d'usage prévue.
// ---------------------------------------------------------------------------------------------

// --- Teintes de marque, version interactive (texte/icône/bouton) ---

/** Violet de marque, assombri depuis #9D84B6 — 5,0:1 sur blanc. */
val BrandPurple = Color(0xFF7A61B0)

/** Rose de marque, assombri depuis #FFD1DC — 4,7:1 sur blanc. */
val BrandRose = Color(0xFFB85078)

/** Menthe de marque, assombrie depuis #B9F6CA — 5,2:1 sur blanc. */
val BrandMint = Color(0xFF257A64)

/** Bleu de marque, assombri depuis #B3E5FC — 5,6:1 sur blanc. */
val BrandBlue = Color(0xFF0B6E99)

// --- Conteneurs pastel (fonds de pastille, chips, badges) ---
val PurpleContainer = Color(0xFFEDE5F8)
val OnPurpleContainer = Color(0xFF2E1C50)
val RoseContainer = Color(0xFFFFD9E4)
val OnRoseContainer = Color(0xFF4A1029)
val MintContainer = Color(0xFFCDF0E2)
val OnMintContainer = Color(0xFF0A3B2E)
val BlueContainer = Color(0xFFDCEFFA)
val OnBlueContainer = Color(0xFF00344A)
val CreamContainer = Color(0xFFFFF3CC)
val OnCreamContainer = Color(0xFF4A3B00)

// --- Neutres clairs, sous-ton rosé/violet (le « papier » de l'app) ---
val CanvasLight = Color(0xFFFFF7F9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFFFFAFC)
val SurfaceContainerLight = Color(0xFFFBF1F5)
val SurfaceContainerHighLight = Color(0xFFF6EAEF)
val SurfaceContainerHighestLight = Color(0xFFF1E3EA)
val SurfaceVariantLight = Color(0xFFF3EBF4)
val OnSurfaceLight = Color(0xFF231A26)
val OnSurfaceVariantLight = Color(0xFF6B5E70)
val OutlineLight = Color(0xFF8E8291)
val OutlineVariantLight = Color(0xFFE7DCE5)

// --- Neutres sombres, même sous-ton violet ---
val CanvasDark = Color(0xFF141018)
val SurfaceContainerLowestDark = Color(0xFF0E0B12)
val SurfaceContainerLowDark = Color(0xFF1B1620)
val SurfaceContainerDark = Color(0xFF1F1925)
val SurfaceContainerHighDark = Color(0xFF2A2331)
val SurfaceContainerHighestDark = Color(0xFF352C3D)
val SurfaceVariantDark = Color(0xFF4A4150)
val OnSurfaceDark = Color(0xFFEFE4EE)
val OnSurfaceVariantDark = Color(0xFFCFC1CE)
val OutlineDark = Color(0xFF988A98)
val OutlineVariantDark = Color(0xFF4A4150)

// --- Teintes de marque en mode sombre (mêmes hues, remontées en luminosité) ---
val BrandPurpleDark = Color(0xFFCDB8EE)
val OnBrandPurpleDark = Color(0xFF3A2065)
val PurpleContainerDark = Color(0xFF523A7C)
val OnPurpleContainerDark = Color(0xFFEADDFF)
val BrandRoseDark = Color(0xFFFFB1C8)
val OnBrandRoseDark = Color(0xFF5A1B37)
val RoseContainerDark = Color(0xFF75324E)
val OnRoseContainerDark = Color(0xFFFFD9E4)
val BrandMintDark = Color(0xFF8CD9C1)
val OnBrandMintDark = Color(0xFF00382B)
val MintContainerDark = Color(0xFF0F5B48)
val OnMintContainerDark = Color(0xFFA9F2DA)

// --- États ---

/** Rouge d'erreur, assombri depuis #E57373 pour rester lisible en texte — 5,0:1 sur blanc. */
val SoftError = Color(0xFFC1443F)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF5F1412)
val SoftErrorDark = Color(0xFFFFB4AB)
val OnSoftErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/**
 * Ambre des péremptions proches (< 7 jours) et des produits entamés. Assombri depuis #E8A342 pour
 * rester lisible en petit texte (5,2:1 sur blanc) ; [WarningContainer] sert de fond de badge.
 */
val WarningOrange = Color(0xFFA15C00)
val WarningContainer = Color(0xFFFFE8C7)
val OnWarningContainer = Color(0xFF3D2000)
val WarningOrangeDark = Color(0xFFFFC46B)
val WarningContainerDark = Color(0xFF5B3700)
val OnWarningContainerDark = Color(0xFFFFE8C7)

/**
 * Pastels d'accent utilisés pour la pastille d'initiale de chaque produit : la teinte est dérivée
 * du nom, ce qui donne un repère visuel stable et rythme la liste sans exiger de saisie.
 */
val AvatarPastels =
    listOf(
        Color(0xFFFFD1DC), // rose d'origine
        Color(0xFFB3E5FC), // bleu ciel d'origine
        Color(0xFFB9F6CA), // menthe d'origine
        Color(0xFFFFF9C4), // crème d'origine
        Color(0xFFE6DCF7), // lavande
        Color(0xFFFFE0C2), // pêche
    )

/** Encre des pastilles d'initiale : un seul encrage foncé, lisible sur les six pastels ci-dessus. */
val AvatarInk = Color(0xFF3B2F43)
