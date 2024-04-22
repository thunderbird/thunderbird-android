package app.k9mail.feature.account.oauth.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@PreviewDevices
internal fun AccountOAuthContentPreview() {
    PreviewWithTheme {
        AccountOAuthContent(
            state = AccountOAuthContract.State(),
            onEvent = {},
        )
    }
}
