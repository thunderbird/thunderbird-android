package app.k9mail.ui.catalog.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.ui.catalog.items.moleculeItems
import app.k9mail.ui.catalog.ui.CatalogContract.State
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.atom.items.buttonItems
import app.k9mail.ui.catalog.ui.atom.items.colorItems
import app.k9mail.ui.catalog.ui.atom.items.iconItems
import app.k9mail.ui.catalog.ui.atom.items.imageItems
import app.k9mail.ui.catalog.ui.atom.items.selectionControlItems
import app.k9mail.ui.catalog.ui.atom.items.textFieldItems
import app.k9mail.ui.catalog.ui.atom.items.typographyItems
import app.k9mail.ui.catalog.ui.common.PagedContent
import app.k9mail.ui.catalog.ui.common.ThemeTopAppBar
import app.k9mail.ui.catalog.ui.common.drawer.DrawerContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CatalogContent(
    state: State,
    pages: ImmutableList<String>,
    onThemeChanged: () -> Unit,
    onThemeVariantChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentPadding = WindowInsets.systemBars.asPaddingValues()

    Scaffold(
        modifier = Modifier
            .padding(top = contentPadding.calculateTopPadding())
            .then(modifier),
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
                onNavigateToAtoms = {},
            )
        },
    ) {
        PagedContent(
            pages = pages,
            initialPage = "Typography",
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (it) {
                "Typography" -> typographyItems()
                "Colors" -> colorItems()
                "Buttons" -> buttonItems()
                "Selection controls" -> selectionControlItems()
                "Text fields" -> textFieldItems()
                "Images" -> imageItems()
                "Icons" -> iconItems()

                "Molecules" -> moleculeItems()

                else -> throw IllegalArgumentException("Unknown page: $it")
            }
        }
    }
}

@DevicePreviews
@Composable
internal fun CatalogContentK9ThemePreview() {
    K9Theme {
        CatalogContent(
            state = State(
                theme = Theme.K9,
                themeVariant = ThemeVariant.LIGHT,
            ),
            pages = persistentListOf("Typography", "Colors"),
            onThemeChanged = {},
            onThemeVariantChanged = {},
            contentPadding = PaddingValues(),
        )
    }
}

@DevicePreviews
@Composable
internal fun CatalogContentThunderbirdThemePreview() {
    ThunderbirdTheme {
        CatalogContent(
            state = State(
                theme = Theme.THUNDERBIRD,
                themeVariant = ThemeVariant.LIGHT,
            ),
            pages = persistentListOf("Typography", "Colors"),
            onThemeChanged = {},
            onThemeVariantChanged = {},
            contentPadding = PaddingValues(),
        )
    }
}
