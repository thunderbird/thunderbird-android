package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun SpecialFoldersFormContentPreview() {
    PreviewWithTheme {
        SpecialFoldersFormContent(
            state = SpecialFoldersContract.FormState(),
            onEvent = {},
        )
    }
}
