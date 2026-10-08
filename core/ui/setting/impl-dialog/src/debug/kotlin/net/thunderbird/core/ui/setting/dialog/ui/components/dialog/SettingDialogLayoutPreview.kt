package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium

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
