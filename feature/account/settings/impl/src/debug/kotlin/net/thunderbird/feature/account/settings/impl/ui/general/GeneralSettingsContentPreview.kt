package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import net.thunderbird.core.ui.setting.dialog.DialogSettingViewProvider
import net.thunderbird.core.ui.setting.emptySettings
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.profile.AccountAvatar

@Composable
@Preview(showBackground = true)
internal fun GeneralSettingsContentPreview() {
    PreviewWithTheme {
        GeneralSettingsContent(
            state = GeneralSettingsContract.State(
                subtitle = "Subtitle",
                name = StringInputField(value = "Alice"),
                color = IntegerInputField(value = 0x112233),
                avatar = AccountAvatar.Monogram("AL"),
            ),
            onEvent = {},
            provider = DialogSettingViewProvider(),
            builder = { emptySettings() },
        )
    }
}
