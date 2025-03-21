package net.thunderbird.ui.catalog.ui.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.common.PagedContent
import net.thunderbird.ui.catalog.ui.molecule.CatalogMoleculePage.INPUT
import net.thunderbird.ui.catalog.ui.molecule.CatalogMoleculePage.PULL_TO_REFRESH
import net.thunderbird.ui.catalog.ui.molecule.CatalogMoleculePage.STATE
import net.thunderbird.ui.catalog.ui.molecule.items.PullToRefresh
import net.thunderbird.ui.catalog.ui.molecule.items.inputItems
import net.thunderbird.ui.catalog.ui.molecule.items.stateItems

@Composable
fun CatalogMoleculeContent(
    pages: ImmutableList<CatalogMoleculePage>,
    initialPage: CatalogMoleculePage,
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
                else -> throw IllegalArgumentException("Unknown page: $it")
            }
        },
        onRenderFullScreenPage = { page ->
            when (page) {
                PULL_TO_REFRESH -> PullToRefresh()
                else -> throw IllegalArgumentException("Unknown page: $page")
            }
        },
    )
}
