package app.k9mail.ui.catalog.ui.common

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import androidx.compose.material.icons.Icons as MaterialIcons

@Composable
fun ThemeTopAppBar(
    onNavigationClick: () -> Unit,
    theme: Theme,
    themeVariant: ThemeVariant,
    onThemeClick: () -> Unit,
    onThemeVariantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = "${theme.displayName} Catalog",
        navigationIcon = {
            ButtonIcon(
                onClick = onNavigationClick,
                imageVector = Icons.Outlined.menu,
            )
        },
        actions = {
            ButtonIcon(
                onClick = onThemeClick,
                imageVector = MaterialIcons.Filled.ShuffleOn,
                contentDescription = "${theme.displayName} Theme",
            )
            ButtonIcon(
                onClick = onThemeVariantClick,
                imageVector = when (themeVariant) {
                    ThemeVariant.LIGHT -> MaterialIcons.Filled.DarkMode
                    ThemeVariant.DARK -> MaterialIcons.Filled.LightMode
                },
                contentDescription = "${theme.displayName} Theme Variant",
            )
        },
        modifier = modifier,
    )
}
