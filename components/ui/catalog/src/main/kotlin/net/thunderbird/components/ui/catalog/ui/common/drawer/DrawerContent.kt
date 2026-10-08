package net.thunderbird.components.ui.catalog.ui.common.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import net.thunderbird.components.ui.bolt.organism.drawer.ModalDrawerSheet
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerDivider
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerHeadline
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerItem
import net.thunderbird.components.ui.catalog.ui.CatalogContract.Theme
import net.thunderbird.components.ui.catalog.ui.CatalogContract.ThemeVariant
import net.thunderbird.components.ui.catalog.ui.next

@Suppress("LongParameterList", "LongMethod")
@Composable
fun DrawerContent(
    closeDrawer: () -> Unit,
    theme: Theme,
    themeVariant: ThemeVariant,
    onThemeChanged: () -> Unit,
    onThemeVariantChanged: () -> Unit,
    onNavigateToAtoms: () -> Unit,
    onNavigateToMolecules: () -> Unit,
    onNavigateToOrganisms: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
    ) {
        NavigationDrawerHeadline(
            title = "Design system",
        )
        NavigationDrawerItem(
            label = "Atoms",
            selected = false,
            onClick = {
                closeDrawer()
                onNavigateToAtoms()
            },
        )
        NavigationDrawerItem(
            label = "Molecules",
            selected = false,
            onClick = {
                closeDrawer()
                onNavigateToMolecules()
            },
        )
        NavigationDrawerItem(
            label = "Organisms",
            selected = false,
            onClick = {
                closeDrawer()
                onNavigateToOrganisms()
            },
        )
        NavigationDrawerItem(
            label = "Templates",
            selected = false,
            onClick = {
                closeDrawer()
                onNavigateToTemplates()
            },
        )

        NavigationDrawerDivider()

        NavigationDrawerHeadline(
            title = "Theme",
        )

        NavigationDrawerItem(
            label = buildAnnotatedString {
                append("Change to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(theme.next().displayName)
                }
                append(" theme")
            },
            selected = false,
            onClick = {
                closeDrawer()
                onThemeChanged()
            },
        )

        NavigationDrawerItem(
            label = buildAnnotatedString {
                append("Change to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(themeVariant.next().displayName)
                }
                append(" theme variant")
            },
            selected = false,
            onClick = {
                closeDrawer()
                onThemeVariantChanged()
            },
        )
    }
}
