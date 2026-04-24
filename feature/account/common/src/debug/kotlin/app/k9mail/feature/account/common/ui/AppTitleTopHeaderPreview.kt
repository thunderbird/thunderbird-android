package app.k9mail.feature.account.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.feature.thundermail.ui.preview.ThundermailPreview

@Composable
@Preview(showBackground = true)
internal fun AppTitleTopHeaderPreview() {
    ThundermailPreview {
        AppTitleTopHeader(title = "Title", sharedTransitionScope = this, animatedVisibilityScope = it)
    }
}
