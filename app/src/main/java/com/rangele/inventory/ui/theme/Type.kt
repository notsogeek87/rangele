package com.rangele.inventory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rangele.inventory.R

/** Varela Round reste la police de marque : c'est elle qui porte la rondeur de l'identité. */
val VarelaRound = FontFamily(Font(R.font.varela_round, FontWeight.Normal))

/**
 * Échelle typographique complète.
 *
 * L'échelle précédente ne définissait que 7 des 15 rôles Material : `labelSmall`, `bodySmall`,
 * `titleSmall`… retombaient sur les valeurs par défaut, donc sur Roboto. Concrètement, la date de
 * péremption et la mention « Entamé » de l'inventaire s'affichaient dans une autre police que le
 * nom du produit juste au-dessus. Tous les rôles sont désormais déclarés en Varela Round.
 *
 * Le rythme est resserré côté titres (interlignage plus court, `letterSpacing` négatif sur les
 * grandes tailles — une police ronde « respire » déjà beaucoup) et aéré côté corps de texte.
 */
val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 54.sp,
                letterSpacing = (-1).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.8).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.6).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.4).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.2).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 23.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.1.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.2.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.2.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = VarelaRound,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
            ),
    )
