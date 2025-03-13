package app.k9mail.ui.catalog.ui.atom

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val NAVIGATION_ROUTE_CATALOG_ATOM = "/catalog/atom"

fun NavController.navigateToCatalogAtom() {
    navigate(
        route = NAVIGATION_ROUTE_CATALOG_ATOM,
        navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build(),
    )
}

fun NavGraphBuilder.catalogAtomRoute(paddingValues: PaddingValues) {
    composable(route = NAVIGATION_ROUTE_CATALOG_ATOM) {
        CatalogAtomScreen(Modifier.padding(paddingValues))
    }
}
