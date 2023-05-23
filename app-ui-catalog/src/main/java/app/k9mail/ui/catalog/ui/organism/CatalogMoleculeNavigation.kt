package app.k9mail.ui.catalog.ui.organism

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val NAVIGATION_ROUTE_CATALOG_ORGANISM = "/catalog/organism"

fun NavController.navigateToCatalogOrganism() {
    navigate(
        route = NAVIGATION_ROUTE_CATALOG_ORGANISM,
        navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build(),
    )
}

fun NavGraphBuilder.catalogOrganismRoute() {
    composable(route = NAVIGATION_ROUTE_CATALOG_ORGANISM) {
        CatalogOrganismScreen()
    }
}
