package net.thunderbird.components.ui.catalog.ui.page.atom

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevicesWithBackground

@Composable
@PreviewDevicesWithBackground
internal fun CatalogContentPreview() {
    PreviewWithTheme {
        CatalogAtomContent(
            pages = persistentListOf(CatalogAtomPage.TYPOGRAPHY, CatalogAtomPage.COLOR),
            initialPage = CatalogAtomPage.TYPOGRAPHY,
            onEvent = {},
        )
    }
}
