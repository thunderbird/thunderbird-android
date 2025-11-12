package net.thunderbird.ui.catalog.ui.page.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract
import net.thunderbird.ui.catalog.ui.page.common.PagedContent
import net.thunderbird.ui.catalog.ui.page.molecule.CatalogMoleculePage.INPUT
import net.thunderbird.ui.catalog.ui.page.molecule.CatalogMoleculePage.PULL_TO_REFRESH
import net.thunderbird.ui.catalog.ui.page.molecule.CatalogMoleculePage.STATE
import net.thunderbird.ui.catalog.ui.page.molecule.CatalogMoleculePage.TAB_ROW
import net.thunderbird.ui.catalog.ui.page.molecule.items.PullToRefresh
import net.thunderbird.ui.catalog.ui.page.molecule.items.inputItems
import net.thunderbird.ui.catalog.ui.page.molecule.items.stateItems
import net.thunderbird.ui.catalog.ui.page.molecule.items.tabRowItems

@Composable
fun CatalogMoleculeContent(
    pages: ImmutableList<CatalogMoleculePage>,
    initialPage: CatalogMoleculePage,
    onEvent: (CatalogPageContract.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    PagedContent(
        pages = pages,
        initialPage = initialPage,
        modifier = modifier,
        onRenderPage = {
            when (it) {
                INPUT -> inputItems()
                STATE -> stateItems()
                TAB_ROW -> tabRowItems()
                else -> throw IllegalArgumentException("Unknown page: $it")
            }
        },
        onRenderFullScreenPage = { page ->
            when (page) {
                PULL_TO_REFRESH -> PullToRefresh()
                else -> throw IllegalArgumentException("Unknown page: $page")
            }
        },
        onEvent = onEvent,
    )
}
