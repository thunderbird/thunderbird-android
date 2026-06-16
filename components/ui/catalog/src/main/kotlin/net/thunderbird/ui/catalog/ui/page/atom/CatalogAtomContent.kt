package net.thunderbird.ui.catalog.ui.page.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.BUTTON
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.CARD
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.COLOR
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.ICON
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.IMAGE
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.SELECTION_CONTROL
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.TAB
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.TEXT_FIELD
import net.thunderbird.ui.catalog.ui.page.atom.CatalogAtomPage.TYPOGRAPHY
import net.thunderbird.ui.catalog.ui.page.atom.items.buttonItems
import net.thunderbird.ui.catalog.ui.page.atom.items.cardItems
import net.thunderbird.ui.catalog.ui.page.atom.items.colorItems
import net.thunderbird.ui.catalog.ui.page.atom.items.iconItems
import net.thunderbird.ui.catalog.ui.page.atom.items.imageItems
import net.thunderbird.ui.catalog.ui.page.atom.items.selectionControlItems
import net.thunderbird.ui.catalog.ui.page.atom.items.tabItems
import net.thunderbird.ui.catalog.ui.page.atom.items.textFieldItems
import net.thunderbird.ui.catalog.ui.page.atom.items.typographyItems
import net.thunderbird.ui.catalog.ui.page.common.PagedContent

@Composable
fun CatalogAtomContent(
    pages: ImmutableList<CatalogAtomPage>,
    initialPage: CatalogAtomPage,
    onEvent: (CatalogPageContract.Event) -> Unit,
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
                CARD -> cardItems()
                TAB -> tabItems()
            }
        },
        onRenderFullScreenPage = {},
        onEvent = onEvent,
    )
}
