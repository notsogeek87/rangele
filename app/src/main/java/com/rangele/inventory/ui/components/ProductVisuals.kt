package com.rangele.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rangele.inventory.ui.theme.AvatarInk
import com.rangele.inventory.ui.theme.AvatarPastels
import com.rangele.inventory.ui.theme.ShapePill
import com.rangele.inventory.ui.theme.ShapeSmall
import com.rangele.inventory.ui.theme.Sizes
import com.rangele.inventory.ui.theme.Spacing
import kotlin.math.absoluteValue

/**
 * Pastille d'initiale d'un produit.
 *
 * Une liste de placard n'a pas de visuel produit à afficher : sans repère, toutes les lignes se
 * ressemblent et l'œil doit relire chaque nom. L'initiale sur un fond pastel dérivé du nom donne
 * une ancre stable (le même produit garde toujours la même couleur) sans rien demander à
 * l'utilisateur.
 */
@Composable
fun ProductAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatar,
) {
    val initial =
        name
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"
    Box(
        modifier = modifier.size(size).background(avatarColorFor(name), ShapeSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = AvatarInk,
        )
    }
}

/** Teinte stable pour [name] : même nom ⇒ même pastel, indépendamment de l'ordre de la liste. */
private fun avatarColorFor(name: String): Color =
    AvatarPastels[name.lowercase().hashCode().absoluteValue % AvatarPastels.size]

/**
 * Badge d'information d'une ligne produit (péremption, « Entamé »…).
 *
 * Ces informations étaient jusqu'ici de simples lignes de texte coloré sous le nom : la couleur y
 * portait seule tout le sens, ce qui la rendait inopérante pour une personne daltonienne et fragile
 * sur un fond clair. Le badge ajoute un fond teinté et une icône, donc deux canaux en plus.
 */
@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = ShapePill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(13.dp)) }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Stepper de quantité groupé : « − valeur + » dans une seule pilule.
 *
 * Les trois contrôles étaient auparavant posés côte à côte, sans lien visuel, au milieu des boutons
 * panier et corbeille — cinq cibles alignées dans la même rangée, dont une destructive collée au
 * « + ». Les regrouper dans un conteneur dit qu'ils forment un seul réglage et éloigne la
 * suppression du geste le plus fréquent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityStepper(
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = ShapePill,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperIcon(
                icon = Icons.Default.Remove,
                contentDescription = "Diminuer la quantité",
                onClick = onDecrement,
            )
            Surface(
                onClick = onValueClick,
                shape = ShapePill,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .widthIn(min = 56.dp)
                            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                    textAlign = TextAlign.Center,
                )
            }
            StepperIcon(
                icon = Icons.Default.Add,
                contentDescription = "Augmenter la quantité",
                onClick = onIncrement,
            )
        }
    }
}

@Composable
private fun StepperIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
