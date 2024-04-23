package app.k9mail.ui.catalog.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.ui.catalog.ui.atom.NAVIGATION_ROUTE_CATALOG_ATOM
import app.k9mail.ui.catalog.ui.atom.catalogAtomRoute
import app.k9mail.ui.catalog.ui.molecule.catalogMoleculeRoute
import app.k9mail.ui.catalog.ui.organism.catalogOrganismRoute

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
