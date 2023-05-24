package app.k9mail.ui.catalog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.ui.catalog.ui.atom.NAVIGATION_ROUTE_CATALOG_ATOM
import app.k9mail.ui.catalog.ui.atom.catalogAtomRoute

@Composable
fun CatalogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NAVIGATION_ROUTE_CATALOG_ATOM,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        catalogAtomRoute()
    }
}
