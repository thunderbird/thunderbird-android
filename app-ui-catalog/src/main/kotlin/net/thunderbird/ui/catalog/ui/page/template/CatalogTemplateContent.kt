package net.thunderbird.ui.catalog.ui.page.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.page.CatalogPageContract
import net.thunderbird.ui.catalog.ui.page.common.PagedContent
import net.thunderbird.ui.catalog.ui.page.template.CatalogTemplatePage.LAYOUT
import net.thunderbird.ui.catalog.ui.page.template.items.layoutItems

@Composable
fun CatalogTemplateContent(
    pages: ImmutableList<CatalogTemplatePage>,
    initialPage: CatalogTemplatePage,
    onEvent: (CatalogPageContract.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    PagedContent(
        pages = pages,
        initialPage = initialPage,
        modifier = modifier,
        onRenderPage = {
            when (it) {
                LAYOUT -> layoutItems()
            }
        },
        onRenderFullScreenPage = {},
        onEvent = onEvent,
    )
}
