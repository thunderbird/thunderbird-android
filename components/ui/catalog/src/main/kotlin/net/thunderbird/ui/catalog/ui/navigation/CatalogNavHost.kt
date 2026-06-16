package net.thunderbird.ui.catalog.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun CatalogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    startDestination: CatalogRoute = CatalogRoute.Atom,
    catalogNavigation: CatalogNavigation = DefaultCatalogNavigation(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.padding(paddingValues),
    ) {
        catalogNavigation.registerRoutes(
            navGraphBuilder = this,
            onBack = { navController.popBackStack() },
            onFinish = { navController.popBackStack() },
        )
    }
}
