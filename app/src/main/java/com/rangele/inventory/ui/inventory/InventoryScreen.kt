package com.rangele.inventory.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rangele.inventory.R
import com.rangele.inventory.data.local.entity.ProductEntity
import com.rangele.inventory.data.repository.ItemDetails
import com.rangele.inventory.ui.components.EmptyState
import com.rangele.inventory.ui.components.ExpirationDateField
import com.rangele.inventory.ui.components.LowStockThresholdField
import com.rangele.inventory.ui.components.NutriscoreBadge
import com.rangele.inventory.ui.components.OpenedCheckbox
import com.rangele.inventory.ui.components.ProductAvatar
import com.rangele.inventory.ui.components.QuantityStepper
import com.rangele.inventory.ui.components.SearchField
import com.rangele.inventory.ui.components.StatusPill
import com.rangele.inventory.ui.theme.RangeleTheme
import com.rangele.inventory.ui.theme.ShapePill
import com.rangele.inventory.ui.theme.ShapeSmall
import com.rangele.inventory.ui.theme.Spacing
import com.rangele.inventory.util.ExpirationStatus
import com.rangele.inventory.util.toEpochMillis
import com.rangele.inventory.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// Format court des badges de la liste : la date y est un repère de tri visuel, pas une donnée à
// recopier — la date complète reste affichée dans la boîte de dialogue d'édition.
private val SHORT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onAddProductClick: () -> Unit,
    onScanBarcodeClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onImportReceiptClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onPantriesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onShoppingListClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val showCreatePantryPrompt by viewModel.showCreatePantryPrompt.collectAsState()
    var productPendingEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productPendingDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchQueryInput by remember { mutableStateOf(uiState.searchQuery) }
    var actionsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mon inventaire",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    // La marque était une icône nue collée au bord ; la poser sur une pastille
                    // teintée lui donne la même présence qu'un avatar d'app et l'aligne sur la
                    // grille des 48dp.
                    Box(
                        modifier =
                            Modifier
                                .padding(start = Spacing.lg, end = Spacing.xs)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, ShapeSmall),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_yakwa_mark),
                            contentDescription = "Yakwa",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Trier")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Trier par ajout récent") },
                            onClick = {
                                viewModel.onSortModeChanged(SortMode.RECENT)
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Trier par nom") },
                            onClick = {
                                viewModel.onSortModeChanged(SortMode.NAME)
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Trier par date de péremption") },
                            onClick = {
                                viewModel.onSortModeChanged(SortMode.EXPIRATION)
                                sortMenuExpanded = false
                            },
                        )
                    }

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Catégories") },
                            onClick = {
                                menuExpanded = false
                                onCategoriesClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Placards") },
                            onClick = {
                                menuExpanded = false
                                onPantriesClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Liste de courses suggérée") },
                            onClick = {
                                menuExpanded = false
                                onShoppingListClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Historique") },
                            onClick = {
                                menuExpanded = false
                                onHistoryClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            },
                        )
                    }
                },
                // La barre se fond dans le canevas au lieu de poser un bandeau crème en haut d'un
                // fond rose : c'est le contenu qui doit marquer la limite, pas un aplat.
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        },
        floatingActionButton = {
            InventoryActionButtons(
                expanded = actionsExpanded,
                onExpandedChange = { actionsExpanded = it },
                onScanBarcodeClick = onScanBarcodeClick,
                onAddProductClick = onAddProductClick,
                onScanReceiptClick = onScanReceiptClick,
                onImportReceiptClick = onImportReceiptClick,
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SearchField(
                value = searchQueryInput,
                onValueChange = {
                    searchQueryInput = it
                    viewModel.onSearchQueryChanged(it)
                },
                placeholder = "Rechercher un produit",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )

            if (uiState.availableCategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                ) {
                    item {
                        CategoryChip(
                            label = "Toutes",
                            selected = uiState.selectedCategory == null,
                            onClick = { viewModel.onCategoryFilterChanged(null) },
                        )
                    }
                    items(uiState.availableCategories) { category ->
                        CategoryChip(
                            label = category,
                            selected = uiState.selectedCategory == category,
                            onClick = {
                                viewModel.onCategoryFilterChanged(
                                    if (uiState.selectedCategory == category) null else category,
                                )
                            },
                        )
                    }
                }
            }

            if (uiState.products.isEmpty() && !uiState.isLoading) {
                if (searchQueryInput.isBlank()) {
                    EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = "Votre placard est vide",
                        description =
                            "Scannez un code-barres, photographiez un ticket de caisse ou ajoutez " +
                                "un produit à la main : il apparaîtra ici.",
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "Aucun résultat",
                        description = "Rien ne correspond à « $searchQueryInput » dans votre inventaire.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Le bas de liste doit dépasser le speed dial, sinon la dernière carte finit
                    // sous le bouton « Scanner un code-barres » et devient intouchable.
                    contentPadding =
                        PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = Spacing.xs,
                            bottom = 96.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            onIncrement = { viewModel.onIncrement(product) },
                            onDecrement = { viewModel.onDecrement(product) },
                            onQuantityClick = { productPendingEdit = product },
                            onShoppingListClick = { viewModel.onToggleShoppingList(product) },
                            onDeleteClick = { productPendingDelete = product },
                        )
                    }
                }
            }
        }
    }

    productPendingEdit?.let { product ->
        if (product.quantityUnit.step == 1.0) {
            val editingItems by viewModel.editingItems.collectAsState()
            LaunchedEffect(product.id) { viewModel.onEditDialogOpened(product.id) }
            editingItems?.let { items ->
                EditItemsDialog(
                    product = product,
                    items = items,
                    onDismiss = {
                        viewModel.onEditDialogClosed()
                        productPendingEdit = null
                    },
                    onConfirm = { items, lowStockThreshold ->
                        viewModel.onItemsSaved(product, items, lowStockThreshold)
                        viewModel.onEditDialogClosed()
                        productPendingEdit = null
                    },
                )
            }
        } else {
            EditQuantityDialog(
                product = product,
                onDismiss = { productPendingEdit = null },
                onConfirm = { newQuantity, expirationDate, opened, lowStockThreshold ->
                    viewModel.onProductSheetSaved(product, newQuantity, expirationDate, opened, lowStockThreshold)
                    productPendingEdit = null
                },
            )
        }
    }

    productPendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Supprimer ${product.name} ?") },
            text = { Text("Ce produit sera retiré de votre inventaire.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDelete(product)
                    productPendingDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) { Text("Annuler") }
            },
        )
    }

    if (showCreatePantryPrompt) {
        CreateFirstPantryDialog(
            onDismiss = viewModel::onDismissCreatePantryPrompt,
            onConfirm = viewModel::onCreateFirstPantry,
        )
    }
}

/**
 * Bouton d'action principal de l'inventaire, en « speed dial ».
 *
 * Les quatre actions étaient auparavant empilées en permanence et mangeaient la moitié basse de
 * l'écran, jusqu'à recouvrir la liste. Seul le scan de code-barres — de loin le geste le plus
 * fréquent pour remplir le placard — reste donc visible et occupe la position la plus basse, la
 * plus facile à atteindre au pouce ; les trois autres se déploient depuis le bouton juste
 * au-dessus, qui se referme d'un second appui.
 */
@Composable
private fun InventoryActionButtons(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onScanBarcodeClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onImportReceiptClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                SpeedDialAction(
                    label = "Ajouter un produit",
                    icon = Icons.Default.Add,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        onExpandedChange(false)
                        onAddProductClick()
                    },
                )
                Spacer(Modifier.height(12.dp))
                SpeedDialAction(
                    label = "Scanner un ticket",
                    icon = Icons.Default.DocumentScanner,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        onExpandedChange(false)
                        onScanReceiptClick()
                    },
                )
                Spacer(Modifier.height(12.dp))
                SpeedDialAction(
                    label = "Importer un ticket",
                    icon = Icons.Default.PhotoLibrary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        onExpandedChange(false)
                        onImportReceiptClick()
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        SmallFloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            shape = ShapePill,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.MoreHoriz,
                contentDescription = if (expanded) "Fermer les autres actions" else "Autres façons d'ajouter",
            )
        }
        Spacer(Modifier.height(12.dp))
        ExtendedFloatingActionButton(
            onClick = onScanBarcodeClick,
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            text = { Text("Scanner un code-barres", style = MaterialTheme.typography.labelLarge) },
            shape = ShapePill,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Action secondaire du speed dial.
 *
 * Le libellé et l'icône vivaient dans deux surfaces distinctes (une étiquette carrée, puis un
 * bouton rond séparé de 12dp) : deux cibles pour une seule action, et deux ombres qui se
 * chevauchaient au-dessus de la liste. Tout tient désormais dans une seule pilule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedDialAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ShapePill,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Chip de filtre par catégorie, sans contour.
 *
 * Le contour par défaut de Material ajoutait un troisième trait (fond + bordure + texte) à une
 * rangée déjà dense ; l'état sélectionné se lit ici au seul contraste de remplissage.
 */
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = ShapePill,
        border = null,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFirstPantryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer votre premier placard") },
        text = {
            Column {
                Text(
                    "Un placard permet de ranger vos produits (ex. Cuisine, Congélateur). " +
                        "Vous pourrez en créer d'autres plus tard depuis le menu « Placards ».",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Nom du placard") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Plus tard") }
        },
    )
}

/**
 * Ligne produit de l'inventaire, en carte à deux étages.
 *
 * L'ancienne ligne alignait cinq cibles tactiles sur une seule rangée (« − », quantité, « + »,
 * panier, corbeille) à droite d'un nom qui n'avait plus que quelques dizaines de dp : sur un
 * téléphone étroit le nom se coupait, et la corbeille touchait le « + ». Les actions sont donc
 * réparties sur deux étages — identité et actions secondaires en haut, état et réglage de quantité
 * en bas — et la quantité est regroupée dans un stepper unique.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductRow(
    product: ProductEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityClick: () -> Unit,
    onShoppingListClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductAvatar(name = product.name, size = 40.dp)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                product.nutriscore?.let { grade ->
                    NutriscoreBadge(grade = grade, modifier = Modifier.padding(horizontal = Spacing.xs))
                }
                IconButton(
                    onClick = onShoppingListClick,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor =
                                if (product.inShoppingList) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        ),
                ) {
                    if (product.inShoppingList) {
                        Icon(
                            Icons.Default.RemoveShoppingCart,
                            contentDescription = "Retirer de la liste de courses",
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = "Ajouter à la liste de courses",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteClick,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // FlowRow plutôt que Row : sur un téléphone étroit, un produit à la fois périmé et
                // entamé aligne deux badges qui, additionnés au stepper, dépassent la largeur de la
                // carte — ils passent alors à la ligne au lieu d'être rognés.
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    product.expirationDate?.let { expirationDate ->
                        ExpirationPill(expirationDate.toLocalDate())
                    }
                    if (product.opened) {
                        StatusPill(
                            label = "Entamé",
                            containerColor = RangeleTheme.accents.warningContainer,
                            contentColor = RangeleTheme.accents.onWarningContainer,
                            icon = Icons.Default.NoFood,
                        )
                    }
                }
                QuantityStepper(
                    valueLabel = formatQuantity(product.quantity, product.quantityUnit.label),
                    onDecrement = onDecrement,
                    onIncrement = onIncrement,
                    onValueClick = onQuantityClick,
                )
            }
        }
    }
}

/**
 * Badge de péremption. L'information passait auparavant par la seule couleur d'un texte de 11sp ;
 * elle est ici portée par trois canaux à la fois — couleur, icône et libellé — pour rester lisible
 * en vision daltonienne comme en plein soleil.
 */
@Composable
private fun ExpirationPill(date: LocalDate) {
    when (ExpirationStatus.of(date)) {
        ExpirationStatus.EXPIRED ->
            StatusPill(
                label = "Périmé ${date.format(SHORT_DATE_FORMAT)}",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.ErrorOutline,
            )
        ExpirationStatus.SOON ->
            StatusPill(
                label = date.format(SHORT_DATE_FORMAT),
                containerColor = RangeleTheme.accents.warningContainer,
                contentColor = RangeleTheme.accents.onWarningContainer,
                icon = Icons.Default.Schedule,
            )
        ExpirationStatus.OK, ExpirationStatus.NONE ->
            StatusPill(
                label = date.format(SHORT_DATE_FORMAT),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.CalendarToday,
            )
    }
}

/** Edits quantity, expiration date and opened status of a product in a continuous unit (weight/volume). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuantityDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, expirationDate: Long?, opened: Boolean, lowStockThreshold: Double?) -> Unit,
) {
    var text by remember(product.id) { mutableStateOf(formatPlainQuantity(product.quantity)) }
    var expirationDate by remember(product.id) { mutableStateOf(product.expirationDate?.toLocalDate()) }
    var opened by remember(product.id) { mutableStateOf(product.opened) }
    var thresholdText by remember(product.id) { mutableStateOf(formatThreshold(product.lowStockThreshold)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${product.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    suffix = { Text(product.quantityUnit.label) },
                )
                ExpirationDateField(
                    date = expirationDate,
                    onDateChanged = { expirationDate = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OpenedCheckbox(
                    opened = opened,
                    onOpenedChanged = { opened = it },
                    modifier = Modifier.padding(top = 4.dp),
                )
                LowStockThresholdField(
                    text = thresholdText,
                    onTextChanged = { thresholdText = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.replace(',', '.').toDoubleOrNull()?.let { quantity ->
                    onConfirm(quantity, expirationDate?.toEpochMillis(), opened, thresholdText.toDoubleOrNull())
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/** Local editable form of [ItemDetails], with [LocalDate] instead of epoch millis for [ExpirationDateField]. */
private data class ItemDraft(
    val date: LocalDate?,
    val opened: Boolean,
)

/**
 * Edits a discrete-unit product (pièce/paquet): one row per unit in stock, each with its own
 * optional expiration date and opened status, instead of a single date/status shared by the whole
 * quantity. Adding or removing a row changes the quantity, down to zero — the product stays in the
 * inventory at zero stock rather than being deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditItemsDialog(
    product: ProductEntity,
    items: List<ItemDetails>,
    onDismiss: () -> Unit,
    onConfirm: (items: List<ItemDetails>, lowStockThreshold: Double?) -> Unit,
) {
    var drafts by
        remember(product.id) {
            mutableStateOf(items.map { ItemDraft(it.expirationDate?.toLocalDate(), it.opened) })
        }
    var thresholdText by remember(product.id) { mutableStateOf(formatThreshold(product.lowStockThreshold)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ${product.name}") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "${drafts.size} ${product.quantityUnit.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                drafts.forEachIndexed { index, draft ->
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExpirationDateField(
                                date = draft.date,
                                onDateChanged = { newDate ->
                                    drafts = drafts.toMutableList().also { it[index] = draft.copy(date = newDate) }
                                },
                                modifier = Modifier.weight(1f),
                                label = "Article ${index + 1}",
                            )
                            IconButton(
                                onClick = { drafts = drafts.toMutableList().also { it.removeAt(index) } },
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Retirer cet article")
                            }
                        }
                        OpenedCheckbox(
                            opened = draft.opened,
                            onOpenedChanged = { newOpened ->
                                drafts = drafts.toMutableList().also { it[index] = draft.copy(opened = newOpened) }
                            },
                        )
                    }
                }
                TextButton(
                    onClick = { drafts = drafts + ItemDraft(date = null, opened = false) },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("+ Ajouter un article")
                }
                LowStockThresholdField(
                    text = thresholdText,
                    onTextChanged = { thresholdText = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val items = drafts.map { ItemDetails(it.date?.toEpochMillis(), it.opened) }
                onConfirm(items, thresholdText.toDoubleOrNull())
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private fun formatPlainQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()

private fun formatThreshold(threshold: Double?): String = threshold?.roundToInt()?.toString() ?: ""

private fun formatQuantity(
    quantity: Double,
    unitLabel: String,
): String = "${formatPlainQuantity(quantity)} $unitLabel"
