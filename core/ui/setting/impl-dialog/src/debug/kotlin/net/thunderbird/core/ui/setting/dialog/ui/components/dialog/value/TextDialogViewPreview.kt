package net.thunderbird.core.ui.setting.dialog.ui.components.dialog.value

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.setting.dialog.ui.fake.FakeSettingData

@Composable
@Preview(showBackground = true)
internal fun TextDialogViewPreview() {
    PreviewWithTheme {
        TextDialogView(
            setting = FakeSettingData.text,
            onConfirmClick = {},
            onDismissClick = {},
            onDismissRequest = {},
        )
    }
}
