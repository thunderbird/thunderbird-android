package app.k9mail.ui.catalog.ui.molecule

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val NAVIGATION_ROUTE_CATALOG_MOLECULE = "/catalog/molecule"

fun NavController.navigateToCatalogMolecule() {
    navigate(
        route = NAVIGATION_ROUTE_CATALOG_MOLECULE,
        navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build(),
    )
}

fun NavGraphBuilder.catalogMoleculeRoute() {
    composable(route = NAVIGATION_ROUTE_CATALOG_MOLECULE) {
        CatalogMoleculeScreen()
    }
}
