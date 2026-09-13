package com.rangele.inventory.ui.navigation

object RangeleDestinations {
    const val INVENTORY = "inventory"
    const val ADD_PRODUCT = "add_product"

    const val SCAN_GRAPH = "scan"
    const val SCAN_CAPTURE = "scan/capture"

    /** Entered straight from the inventory: opens the file picker without going through the camera. */
    const val SCAN_IMPORT = "scan/import"
    const val SCAN_REVIEW = "scan/review"

    const val BARCODE_GRAPH = "barcode"
    const val BARCODE_SCAN = "barcode/scan"
    const val BARCODE_RESULT = "barcode/result"

    const val CATEGORIES = "categories"
    const val PANTRIES = "pantries"
    const val HISTORY = "history"
    const val SHOPPING_LIST = "shopping_list"
    const val SETTINGS = "settings"
}
