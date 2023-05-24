package app.k9mail.ui.catalog.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline4
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.ui.catalog.items.moleculeItems
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
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.theme.ThemeSelector
import app.k9mail.ui.catalog.ui.common.theme.ThemeVariantSelector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CatalogContent(
    pages: ImmutableList<String>,
    theme: Theme,
    themeVariant: ThemeVariant,
    onThemeChange: () -> Unit,
    onThemeVariantChange: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(contentPadding),
    ) {
        TextHeadline4(
            text = "Thunderbird Catalog",
            modifier = Modifier
                .padding(
                    start = MainTheme.spacings.double,
                    top = MainTheme.spacings.default,
                    end = MainTheme.spacings.double,
                )
                .fillMaxWidth(),
        )
        ThemeSelector(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .itemDefaultPadding(),
            onThemeChangeClick = onThemeChange,
        )
        ThemeVariantSelector(
            themeVariant = themeVariant,
            modifier = Modifier
                .fillMaxWidth()
                .itemDefaultPadding(),
            onThemeVariantChange = onThemeVariantChange,
        )
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
            pages = persistentListOf("Typography", "Colors"),
            theme = Theme.K9,
            themeVariant = ThemeVariant.LIGHT,
            onThemeChange = {},
            onThemeVariantChange = {},
            contentPadding = PaddingValues(),
        )
    }
}

@DevicePreviews
@Composable
internal fun CatalogContentThunderbirdThemePreview() {
    ThunderbirdTheme {
        CatalogContent(
            pages = persistentListOf("Typography", "Colors"),
            theme = Theme.THUNDERBIRD,
            themeVariant = ThemeVariant.LIGHT,
            onThemeChange = {},
            onThemeVariantChange = {},
            contentPadding = PaddingValues(),
        )
    }
}
