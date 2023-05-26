package app.k9mail.ui.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.ui.catalog.ui.CatalogContract.State
import app.k9mail.ui.catalog.ui.atom.navigateToCatalogAtom
import app.k9mail.ui.catalog.ui.common.ThemeTopAppBar
import app.k9mail.ui.catalog.ui.common.drawer.DrawerContent
import app.k9mail.ui.catalog.ui.molecule.navigateToCatalogMolecule
import app.k9mail.ui.catalog.ui.navigation.CatalogNavHost
import app.k9mail.ui.catalog.ui.organism.navigateToCatalogOrganism

@Composable
fun CatalogContent(
    state: State,
    onThemeChanged: () -> Unit,
    onThemeVariantChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        topBar = { toggleDrawer ->
            ThemeTopAppBar(
                onNavigationClick = toggleDrawer,
                theme = state.theme,
                themeVariant = state.themeVariant,
                onThemeClick = onThemeChanged,
                onThemeVariantClick = onThemeVariantChanged,
            )
        },
        drawerContent = { toogleDrawer ->
            DrawerContent(
                toggleDrawer = toogleDrawer,
                theme = state.theme,
                themeVariant = state.themeVariant,
                onThemeChanged = onThemeChanged,
                onThemeVariantChanged = onThemeVariantChanged,
                onNavigateToAtoms = {
                    navController.navigateToCatalogAtom()
                },
                onNavigateToMolecules = {
                    navController.navigateToCatalogMolecule()
                },
                onNavigateToOrganisms = {
                    navController.navigateToCatalogOrganism()
                },
            )
        },
    ) {
        CatalogNavHost(
            navController = navController,
        )
    }
}
