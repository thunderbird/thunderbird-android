package app.k9mail.ui.catalog.ui.common.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.common.theme.ThemeSelector
import app.k9mail.ui.catalog.ui.common.theme.ThemeVariantSelector

@Suppress("LongParameterList", "LongMethod")
@Composable
fun DrawerContent(
    toggleDrawer: () -> Unit,
    theme: Theme,
    themeVariant: ThemeVariant,
    onThemeChanged: () -> Unit,
    onThemeVariantChanged: () -> Unit,
    onNavigateToAtoms: () -> Unit,
    onNavigateToMolecules: () -> Unit,
    onNavigateToOrganisms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        item {
            DrawerHeaderItem(
                text = "Design system",
            )
        }
        item {
            DrawerCategoryItem(
                text = "Atoms",
                onItemClick = {
                    toggleDrawer()
                    onNavigateToAtoms()
                },
            )
        }
        item {
            DrawerCategoryItem(
                text = "Molecules",
                onItemClick = {
                    toggleDrawer()
                    onNavigateToMolecules()
                },
            )
        }
        item {
            DrawerCategoryItem(
                text = "Organisms",
                onItemClick = {
                    toggleDrawer()
                    onNavigateToOrganisms()
                },
            )
        }

        item {
            DrawerHeaderItem(
                text = "Theme selection",
            )
        }
        item {
            ThemeSelector(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MainTheme.spacings.double),
                onThemeChangeClick = onThemeChanged,
            )
        }
        item {
            ThemeVariantSelector(
                themeVariant = themeVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MainTheme.spacings.double),
                onThemeVariantChange = onThemeVariantChanged,
            )
        }
    }
}
