package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AppTitleTopHeaderPreview() {
    PreviewWithThemes {
        AppTitleTopHeader(title = "Title")
    }
}
