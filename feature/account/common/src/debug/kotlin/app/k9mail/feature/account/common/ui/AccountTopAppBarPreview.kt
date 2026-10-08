package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun AccountTopAppBarPreview() {
    PreviewWithThemes {
        AccountTopAppBar(
            title = "Title",
        )
    }
}
