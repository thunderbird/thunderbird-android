package net.thunderbird.ui.catalog.ui.molecule

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
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

fun NavGraphBuilder.catalogMoleculeRoute(paddingValues: PaddingValues) {
    composable(route = NAVIGATION_ROUTE_CATALOG_MOLECULE) {
        CatalogMoleculeScreen(Modifier.padding(paddingValues))
    }
}
