package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@PreviewDevices
internal fun AccountTopAppBarPreview() {
    PreviewWithThemes {
        AccountTopAppBar(
            title = "Title",
        )
    }
}
