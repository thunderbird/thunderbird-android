package net.thunderbird.ui.catalog.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import net.thunderbird.ui.catalog.ui.atom.NAVIGATION_ROUTE_CATALOG_ATOM
import net.thunderbird.ui.catalog.ui.atom.catalogAtomRoute
import net.thunderbird.ui.catalog.ui.molecule.catalogMoleculeRoute
import net.thunderbird.ui.catalog.ui.organism.catalogOrganismRoute

@Composable
fun CatalogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    startDestination: String = NAVIGATION_ROUTE_CATALOG_ATOM,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        catalogAtomRoute(paddingValues)
        catalogMoleculeRoute(paddingValues)
        catalogOrganismRoute(paddingValues)
    }
}
