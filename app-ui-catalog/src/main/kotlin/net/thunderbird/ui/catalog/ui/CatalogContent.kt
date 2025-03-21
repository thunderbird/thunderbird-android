package net.thunderbird.ui.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import app.k9mail.core.ui.compose.designsystem.organism.drawer.ModalNavigationDrawer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import net.thunderbird.ui.catalog.ui.CatalogContract.State
import net.thunderbird.ui.catalog.ui.common.ThemeTopAppBar
import net.thunderbird.ui.catalog.ui.common.drawer.DrawerContent
import net.thunderbird.ui.catalog.ui.navigation.CatalogNavHost
import net.thunderbird.ui.catalog.ui.navigation.CatalogRoute

@Suppress("LongMethod")
@Composable
fun CatalogContent(
    navController: NavHostController,
    state: State,
    onThemeChanged: () -> Unit,
    onThemeVariantChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalNavigationDrawer(
        drawerContent = { closeDrawer ->
            DrawerContent(
                closeDrawer = closeDrawer,
                theme = state.theme,
                themeVariant = state.themeVariant,
                onThemeChanged = onThemeChanged,
                onThemeVariantChanged = onThemeVariantChanged,
                onNavigateToAtoms = {
                    navController.navigate(
                        route = CatalogRoute.Atom,
                        navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build(),
                    )
                },
                onNavigateToMolecules = {
                    navController.navigate(
                        route = CatalogRoute.Molecule,
                        navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build(),
                    )
                },
                onNavigateToOrganisms = {
                    navController.navigate(
                        route = CatalogRoute.Organism,
                        navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build(),
                    )
                },
                onNavigateToTemplates = {
                    navController.navigate(
                        route = CatalogRoute.Template,
                        navOptions = NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .build(),
                    )
                },
            )
        },
    ) { openDrawer ->
        Scaffold(
            modifier = modifier,
            topBar = {
                ThemeTopAppBar(
                    onMenuClick = openDrawer,
                    theme = state.theme,
                    themeVariant = state.themeVariant,
                    onThemeClick = onThemeChanged,
                    onThemeVariantClick = onThemeVariantChanged,
                )
            },
        ) { paddingValues ->
            CatalogNavHost(
                navController = navController,
                paddingValues = paddingValues,
            )
        }
    }
}
