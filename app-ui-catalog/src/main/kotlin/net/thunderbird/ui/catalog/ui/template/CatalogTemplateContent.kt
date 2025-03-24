package net.thunderbird.ui.catalog.ui.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.ui.catalog.ui.common.PagedContent
import net.thunderbird.ui.catalog.ui.template.CatalogTemplatePage.LAYOUT
import net.thunderbird.ui.catalog.ui.template.items.layoutItems

@Composable
fun CatalogTemplateContent(
    pages: ImmutableList<CatalogTemplatePage>,
    initialPage: CatalogTemplatePage,
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
    )
}
