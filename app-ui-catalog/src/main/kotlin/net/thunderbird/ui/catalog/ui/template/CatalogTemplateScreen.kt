package net.thunderbird.ui.catalog.ui.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CatalogTemplateScreen(
    modifier: Modifier = Modifier,
) {
    CatalogTemplateContent(
        pages = CatalogTemplatePage.Companion.all(),
        initialPage = CatalogTemplatePage.LAYOUT,
        modifier = modifier,
    )
}
