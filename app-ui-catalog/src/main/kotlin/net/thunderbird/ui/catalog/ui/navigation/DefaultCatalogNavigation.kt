package net.thunderbird.ui.catalog.ui.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomScreen
import net.thunderbird.ui.catalog.ui.molecule.CatalogMoleculeScreen
import net.thunderbird.ui.catalog.ui.organism.CatalogOrganismScreen

class DefaultCatalogNavigation : CatalogNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (CatalogRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<CatalogRoute.Atom>(
                basePath = CatalogRoute.Atom.BASE_PATH,
            ) { backStackEntry ->
                CatalogAtomScreen()
            }

            deepLinkComposable<CatalogRoute.Molecule>(
                basePath = CatalogRoute.Molecule.BASE_PATH,
            ) { backStackEntry ->
                CatalogMoleculeScreen()
            }

            deepLinkComposable<CatalogRoute.Organism>(
                basePath = CatalogRoute.Organism.BASE_PATH,
            ) { backStackEntry ->
                CatalogOrganismScreen()
            }
        }
    }
}
