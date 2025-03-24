package net.thunderbird.ui.catalog.ui.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CatalogMoleculeScreen(
    modifier: Modifier = Modifier,
) {
    CatalogMoleculeContent(
        pages = CatalogMoleculePage.all(),
        initialPage = CatalogMoleculePage.INPUT,
        modifier = modifier,
    )
}
