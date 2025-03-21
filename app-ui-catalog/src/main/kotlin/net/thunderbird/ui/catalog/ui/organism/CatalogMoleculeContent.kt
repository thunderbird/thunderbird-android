package net.thunderbird.ui.catalog.ui.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.common.PagedContent
import net.thunderbird.ui.catalog.ui.organism.CatalogOrganismPage.APP_BAR
import net.thunderbird.ui.catalog.ui.organism.CatalogOrganismPage.DIALOG
import net.thunderbird.ui.catalog.ui.organism.items.appBarItems
import net.thunderbird.ui.catalog.ui.organism.items.dialogItems

@Composable
fun CatalogOrganismContent(
    pages: ImmutableList<CatalogOrganismPage>,
    initialPage: CatalogOrganismPage,
    modifier: Modifier = Modifier,
) {
    PagedContent(
        pages = pages,
        initialPage = initialPage,
        modifier = modifier,
        onRenderPage = {
            when (it) {
                APP_BAR -> appBarItems()
                DIALOG -> dialogItems()
            }
        },
        onRenderFullScreenPage = {},
    )
}
