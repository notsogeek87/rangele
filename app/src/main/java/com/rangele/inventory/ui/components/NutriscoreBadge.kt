package com.rangele.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Couleurs officielles Nutri-Score, indépendantes du thème clair/sombre de l'app. */
private val NutriscoreColors =
    mapOf(
        "a" to Color(0xFF038141),
        "b" to Color(0xFF85BB2F),
        "c" to Color(0xFFFECB02),
        "d" to Color(0xFFEE8100),
        "e" to Color(0xFFE63E11),
    )

/** Petit badge rond affichant le grade Nutri-Score ("a" à "e"). N'affiche rien si [grade] est inconnu. */
@Composable
fun NutriscoreBadge(
    grade: String?,
    modifier: Modifier = Modifier,
) {
    val normalized = grade?.lowercase()
    val color = NutriscoreColors[normalized] ?: return
    Box(
        modifier = modifier.size(20.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = normalized.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
