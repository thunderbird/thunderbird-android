package app.k9mail.ui.catalog.ui.organism

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CatalogOrganismScreen(
    modifier: Modifier = Modifier,
) {
    CatalogOrganismContent(
        pages = CatalogOrganismPage.all(),
        initialPage = CatalogOrganismPage.APP_BAR,
        modifier = modifier,
    )
}
