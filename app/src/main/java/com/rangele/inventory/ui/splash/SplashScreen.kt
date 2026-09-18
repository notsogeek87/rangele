package com.rangele.inventory.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rangele.inventory.R
import com.rangele.inventory.ui.theme.ShapeExtraLarge
import com.rangele.inventory.ui.theme.Spacing
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 900L

/**
 * Écran d'ouverture.
 *
 * Le logo flottait seul au centre d'un aplat rose, sans nom d'app ni promesse : rien n'y distinguait
 * un lancement réussi d'un écran resté bloqué. Le dégradé de marque, le médaillon et la baseline
 * donnent un point de départ tenu, et le médaillon clair garantit que le logo reste lisible quel que
 * soit le thème clair/sombre du système.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onTimeout()
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = ShapeExtraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 12.dp,
            ) {
                Image(
                    painter = painterResource(R.drawable.img_yakwa_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.padding(Spacing.xl).size(180.dp),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Ce qu'il y a dans vos placards, toujours à jour",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.xxl),
            )
        }
    }
}
