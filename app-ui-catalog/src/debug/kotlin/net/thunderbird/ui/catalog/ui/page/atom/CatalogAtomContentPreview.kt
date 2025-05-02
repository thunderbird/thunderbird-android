package net.thunderbird.ui.catalog.ui.page.atom

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf

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
