package net.thunderbird.ui.catalog.ui.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.BUTTON
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.COLOR
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.ICON
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.IMAGE
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.SELECTION_CONTROL
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.TEXT_FIELD
import net.thunderbird.ui.catalog.ui.atom.CatalogAtomPage.TYPOGRAPHY
import net.thunderbird.ui.catalog.ui.atom.items.buttonItems
import net.thunderbird.ui.catalog.ui.atom.items.colorItems
import net.thunderbird.ui.catalog.ui.atom.items.iconItems
import net.thunderbird.ui.catalog.ui.atom.items.imageItems
import net.thunderbird.ui.catalog.ui.atom.items.selectionControlItems
import net.thunderbird.ui.catalog.ui.atom.items.textFieldItems
import net.thunderbird.ui.catalog.ui.atom.items.typographyItems
import net.thunderbird.ui.catalog.ui.common.PagedContent

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
        onRenderPage = {
            when (it) {
                TYPOGRAPHY -> typographyItems()
                COLOR -> colorItems()
                BUTTON -> buttonItems()
                SELECTION_CONTROL -> selectionControlItems()
                TEXT_FIELD -> textFieldItems()
                ICON -> iconItems()
                IMAGE -> imageItems()
            }
        },
        onRenderFullScreenPage = {},
    )
}
