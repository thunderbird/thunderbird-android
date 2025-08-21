package net.thunderbird.ui.catalog.ui.page.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract
import net.thunderbird.ui.catalog.ui.page.common.PagedContent
import net.thunderbird.ui.catalog.ui.page.organism.CatalogOrganismPage.APP_BAR
import net.thunderbird.ui.catalog.ui.page.organism.CatalogOrganismPage.BANNER
import net.thunderbird.ui.catalog.ui.page.organism.CatalogOrganismPage.DIALOG
import net.thunderbird.ui.catalog.ui.page.organism.CatalogOrganismPage.SNACKBAR
import net.thunderbird.ui.catalog.ui.page.organism.items.SnackbarItems
import net.thunderbird.ui.catalog.ui.page.organism.items.appBarItems
import net.thunderbird.ui.catalog.ui.page.organism.items.bannerItems
import net.thunderbird.ui.catalog.ui.page.organism.items.dialogItems

@Composable
fun CatalogOrganismContent(
    pages: ImmutableList<CatalogOrganismPage>,
    initialPage: CatalogOrganismPage,
    onEvent: (CatalogPageContract.Event) -> Unit,
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
                BANNER -> bannerItems()
                SNACKBAR -> Unit
            }
        },
        onRenderFullScreenPage = {
            when (it) {
                SNACKBAR -> SnackbarItems()
                else -> Unit
            }
        },
        onEvent = onEvent,
    )
}
