package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.common.annotation.PreviewDevices

@Composable
@PreviewDevices
internal fun AccountTopAppBarPreview() {
    PreviewWithThemes {
        AccountTopAppBar(
            title = "Title",
        )
    }
}
