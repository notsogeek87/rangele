package com.rangele.inventory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Kawaii Pastel Pop is a light-only aesthetic by design (no dark variant), so the app always
// renders with this palette regardless of the system theme or Material You dynamic color.
private val KawaiiColorScheme =
    lightColorScheme(
        primary = MutedPurple,
        onPrimary = BabyBlue,
        primaryContainer = PurpleContainer,
        onPrimaryContainer = OnPurpleContainer,
        secondary = BabyBlue,
        onSecondary = OffBlack,
        secondaryContainer = BlueContainer,
        onSecondaryContainer = OffBlack,
        tertiary = Mint,
        onTertiary = OffBlack,
        background = PastelPink,
        onBackground = OffBlack,
        surface = Cream,
        onSurface = OffBlack,
        surfaceVariant = CreamVariant,
        onSurfaceVariant = OnCreamVariant,
        outline = Outline,
        error = SoftError,
        onError = Color.White,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
    )

@Composable
fun RangeleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KawaiiColorScheme,
        typography = Typography,
        shapes = KawaiiShapes,
        content = content,
    )
}
