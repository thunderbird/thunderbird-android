package net.thunderbird.core.ui.compose.preference.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium

@Composable
@Preview(showBackground = true)
internal fun PreferenceDialogLayoutPreview() {
    PreviewWithTheme {
        PreferenceDialogLayout(
            title = "Dialog",
            icon = null,
            onConfirmClick = {},
            onDismissClick = {},
            onDismissRequest = {},
        ) {
            TextBodyMedium("PreferenceDialogLayoutContent")
        }
    }
}
