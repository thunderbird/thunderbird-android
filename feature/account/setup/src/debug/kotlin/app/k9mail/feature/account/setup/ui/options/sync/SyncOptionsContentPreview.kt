package app.k9mail.feature.account.setup.ui.options.sync

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.feature.account.common.ui.PreviewWithThemeAndKoin

@Composable
@Preview(showBackground = true)
internal fun SyncOptionsContentPreview() {
    PreviewWithThemeAndKoin {
        SyncOptionsContent(
            state = SyncOptionsContract.State(),
            onEvent = {},
            contentPadding = PaddingValues(),
        )
    }
}
