package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersFormContentPreview() {
    PreviewWithTheme {
        SpecialFoldersFormContent(
            state = SpecialFoldersContract.FormState(),
            onEvent = {},
            maxWidth = Dp.Unspecified,
        )
    }
}
