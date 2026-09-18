package com.rangele.inventory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(
        primary = BrandPurple,
        onPrimary = Color.White,
        primaryContainer = PurpleContainer,
        onPrimaryContainer = OnPurpleContainer,
        inversePrimary = BrandPurpleDark,
        secondary = BrandRose,
        onSecondary = Color.White,
        secondaryContainer = RoseContainer,
        onSecondaryContainer = OnRoseContainer,
        tertiary = BrandMint,
        onTertiary = Color.White,
        tertiaryContainer = MintContainer,
        onTertiaryContainer = OnMintContainer,
        background = CanvasLight,
        onBackground = OnSurfaceLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        surfaceTint = BrandPurple,
        inverseSurface = Color(0xFF382F3C),
        inverseOnSurface = Color(0xFFFBEDF5),
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        scrim = Color(0xFF231A26),
        error = SoftError,
        onError = Color.White,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = BrandPurpleDark,
        onPrimary = OnBrandPurpleDark,
        primaryContainer = PurpleContainerDark,
        onPrimaryContainer = OnPurpleContainerDark,
        inversePrimary = BrandPurple,
        secondary = BrandRoseDark,
        onSecondary = OnBrandRoseDark,
        secondaryContainer = RoseContainerDark,
        onSecondaryContainer = OnRoseContainerDark,
        tertiary = BrandMintDark,
        onTertiary = OnBrandMintDark,
        tertiaryContainer = MintContainerDark,
        onTertiaryContainer = OnMintContainerDark,
        background = CanvasDark,
        onBackground = OnSurfaceDark,
        surface = SurfaceContainerLowDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        surfaceTint = BrandPurpleDark,
        inverseSurface = OnSurfaceDark,
        inverseOnSurface = CanvasDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        scrim = Color.Black,
        error = SoftErrorDark,
        onError = OnSoftErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
    )

/**
 * Couleurs métier qui n'ont pas de rôle Material : l'ambre des péremptions proches et des produits
 * entamés. Passer par un [staticCompositionLocalOf] plutôt que par une constante globale permet de
 * les basculer en même temps que le reste du thème.
 */
data class RangeleAccents(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

private val LightAccents =
    RangeleAccents(
        warning = WarningOrange,
        warningContainer = WarningContainer,
        onWarningContainer = OnWarningContainer,
    )

private val DarkAccents =
    RangeleAccents(
        warning = WarningOrangeDark,
        warningContainer = WarningContainerDark,
        onWarningContainer = OnWarningContainerDark,
    )

private val LocalRangeleAccents = staticCompositionLocalOf { LightAccents }

/** Accès aux couleurs d'accent hors palette Material, sur le modèle de `MaterialTheme.colorScheme`. */
object RangeleTheme {
    val accents: RangeleAccents
        @Composable
        @ReadOnlyComposable
        get() = LocalRangeleAccents.current
}

@Composable
fun RangeleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRangeleAccents provides if (darkTheme) DarkAccents else LightAccents) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = RangeleShapes,
            content = content,
        )
    }
}
