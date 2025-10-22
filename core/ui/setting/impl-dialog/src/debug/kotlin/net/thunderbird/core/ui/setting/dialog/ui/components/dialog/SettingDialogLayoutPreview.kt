package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium

@Composable
@Preview(showBackground = true)
internal fun SettingDialogLayoutPreview() {
    PreviewWithTheme {
        SettingDialogLayout(
            title = "Dialog",
            icon = null,
            onConfirmClick = {},
            onDismissClick = {},
            onDismissRequest = {},
        ) {
            TextBodyMedium("SettingDialogLayoutContent")
        }
    }
}
