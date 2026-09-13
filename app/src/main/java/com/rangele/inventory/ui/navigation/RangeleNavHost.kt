package com.rangele.inventory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.rangele.inventory.AppContainer
import com.rangele.inventory.ui.addproduct.AddProductScreen
import com.rangele.inventory.ui.addproduct.AddProductViewModel
import com.rangele.inventory.ui.categories.CategoriesScreen
import com.rangele.inventory.ui.categories.CategoriesViewModel
import com.rangele.inventory.ui.history.HistoryScreen
import com.rangele.inventory.ui.history.HistoryViewModel
import com.rangele.inventory.ui.inventory.InventoryScreen
import com.rangele.inventory.ui.inventory.InventoryViewModel
import com.rangele.inventory.ui.scan.ReceiptImportScreen
import com.rangele.inventory.ui.scan.ReceiptReviewScreen
import com.rangele.inventory.ui.scan.ScanCaptureScreen
import com.rangele.inventory.ui.scan.ScanViewModel
import com.rangele.inventory.ui.settings.SettingsScreen
import com.rangele.inventory.ui.settings.SettingsViewModel
import com.rangele.inventory.ui.shoppinglist.ShoppingListScreen
import com.rangele.inventory.ui.shoppinglist.ShoppingListViewModel

@Composable
fun RangeleNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = RangeleDestinations.INVENTORY) {
        composable(RangeleDestinations.INVENTORY) { entry ->
            val viewModel: InventoryViewModel =
                viewModel(
                    entry,
                    factory =
                        viewModelFactory {
                            initializer {
                                InventoryViewModel(container.inventoryRepository, container.categoryRepository)
                            }
                        },
                )
            InventoryScreen(
                viewModel = viewModel,
                onAddProductClick = { navController.navigate(RangeleDestinations.ADD_PRODUCT) },
                onScanReceiptClick = { navController.navigate(RangeleDestinations.SCAN_GRAPH) },
                onImportReceiptClick = { navController.navigate(RangeleDestinations.SCAN_IMPORT) },
                onCategoriesClick = { navController.navigate(RangeleDestinations.CATEGORIES) },
                onHistoryClick = { navController.navigate(RangeleDestinations.HISTORY) },
                onShoppingListClick = { navController.navigate(RangeleDestinations.SHOPPING_LIST) },
                onSettingsClick = { navController.navigate(RangeleDestinations.SETTINGS) },
            )
        }

        composable(RangeleDestinations.ADD_PRODUCT) { entry ->
            val viewModel: AddProductViewModel =
                viewModel(
                    entry,
                    factory =
                        viewModelFactory {
                            initializer {
                                AddProductViewModel(container.inventoryRepository, container.categoryRepository)
                            }
                        },
                )
            AddProductScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        navigation(startDestination = RangeleDestinations.SCAN_CAPTURE, route = RangeleDestinations.SCAN_GRAPH) {
            composable(RangeleDestinations.SCAN_CAPTURE) { entry ->
                val scanViewModel = rememberScanViewModel(navController, entry, container)
                ScanCaptureScreen(
                    viewModel = scanViewModel,
                    onBackClick = { navController.popBackStack(RangeleDestinations.INVENTORY, inclusive = false) },
                    onCaptured = { navController.navigate(RangeleDestinations.SCAN_REVIEW) },
                )
            }

            composable(RangeleDestinations.SCAN_IMPORT) { entry ->
                val scanViewModel = rememberScanViewModel(navController, entry, container)
                ReceiptImportScreen(
                    viewModel = scanViewModel,
                    // Dropped from the back stack: coming back from the review screen would otherwise
                    // land here and immediately reopen the picker.
                    onImported = {
                        navController.navigate(RangeleDestinations.SCAN_REVIEW) {
                            popUpTo(RangeleDestinations.SCAN_IMPORT) { inclusive = true }
                        }
                    },
                    onCancelled = { navController.popBackStack(RangeleDestinations.INVENTORY, inclusive = false) },
                )
            }

            composable(RangeleDestinations.SCAN_REVIEW) { entry ->
                val scanViewModel = rememberScanViewModel(navController, entry, container)
                ReceiptReviewScreen(
                    viewModel = scanViewModel,
                    onBackClick = { navController.popBackStack() },
                    onImported = { navController.popBackStack(RangeleDestinations.INVENTORY, inclusive = false) },
                )
            }
        }

        composable(RangeleDestinations.CATEGORIES) { entry ->
            val viewModel: CategoriesViewModel =
                viewModel(
                    entry,
                    factory = viewModelFactory { initializer { CategoriesViewModel(container.categoryRepository) } },
                )
            CategoriesScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }

        composable(RangeleDestinations.HISTORY) { entry ->
            val viewModel: HistoryViewModel =
                viewModel(
                    entry,
                    factory = viewModelFactory { initializer { HistoryViewModel(container.historyRepository) } },
                )
            HistoryScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }

        composable(RangeleDestinations.SHOPPING_LIST) { entry ->
            val viewModel: ShoppingListViewModel =
                viewModel(
                    entry,
                    factory = viewModelFactory { initializer { ShoppingListViewModel(container.inventoryRepository) } },
                )
            ShoppingListScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }

        composable(RangeleDestinations.SETTINGS) { entry ->
            val viewModel: SettingsViewModel =
                viewModel(
                    entry,
                    factory =
                        viewModelFactory {
                            initializer {
                                SettingsViewModel(container.settingsRepository, container.expirationCheckScheduler)
                            }
                        },
                )
            SettingsScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
    }
}

@Composable
private fun rememberScanViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
    container: AppContainer,
): ScanViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(RangeleDestinations.SCAN_GRAPH) }
    return viewModel(
        parentEntry,
        factory =
            viewModelFactory {
                initializer {
                    ScanViewModel(
                        container.inventoryRepository,
                        container.receiptTextRecognizer,
                        container.receiptParser,
                    )
                }
            },
    )
}
