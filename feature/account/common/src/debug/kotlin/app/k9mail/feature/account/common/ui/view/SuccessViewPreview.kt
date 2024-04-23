package app.k9mail.feature.account.common.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SuccessViewPreview() {
    PreviewWithThemes {
        SuccessView(
            message = "The app tried really hard and managed to successfully complete the operation.",
        )
    }
}
