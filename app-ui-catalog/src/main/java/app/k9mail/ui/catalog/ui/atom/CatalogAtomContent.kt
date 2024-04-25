package app.k9mail.ui.catalog.ui.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.BUTTON
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.COLOR
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.ICON
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.IMAGE
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.SELECTION_CONTROL
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.TEXT_FIELD
import app.k9mail.ui.catalog.ui.atom.CatalogAtomPage.TYPOGRAPHY
import app.k9mail.ui.catalog.ui.atom.items.buttonItems
import app.k9mail.ui.catalog.ui.atom.items.colorItems
import app.k9mail.ui.catalog.ui.atom.items.iconItems
import app.k9mail.ui.catalog.ui.atom.items.imageItems
import app.k9mail.ui.catalog.ui.atom.items.selectionControlItems
import app.k9mail.ui.catalog.ui.atom.items.textFieldItems
import app.k9mail.ui.catalog.ui.atom.items.typographyItems
import app.k9mail.ui.catalog.ui.common.PagedContent
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CatalogAtomContent(
    pages: ImmutableList<CatalogAtomPage>,
    initialPage: CatalogAtomPage,
    modifier: Modifier = Modifier,
) {
    PagedContent(
        pages = pages,
        initialPage = initialPage,
        modifier = modifier,
    ) {
        when (it) {
            TYPOGRAPHY -> typographyItems()
            COLOR -> colorItems()
            BUTTON -> buttonItems()
            SELECTION_CONTROL -> selectionControlItems()
            TEXT_FIELD -> textFieldItems()
            ICON -> iconItems()
            IMAGE -> imageItems()
        }
    }
}
