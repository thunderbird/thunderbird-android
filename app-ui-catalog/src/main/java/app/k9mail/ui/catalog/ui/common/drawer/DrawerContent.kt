package app.k9mail.ui.catalog.ui.common.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.k9mail.core.ui.compose.designsystem.organism.drawer.ModalDrawerSheet
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerDivider
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerHeadline
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItem
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.next

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
