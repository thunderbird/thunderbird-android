package app.k9mail.ui.catalog.ui.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CatalogAtomScreen(
    modifier: Modifier = Modifier,
) {
    CatalogAtomContent(
        pages = CatalogAtomPage.all(),
        initialPage = CatalogAtomPage.TYPOGRAPHY,
        modifier = modifier,
    )
}
