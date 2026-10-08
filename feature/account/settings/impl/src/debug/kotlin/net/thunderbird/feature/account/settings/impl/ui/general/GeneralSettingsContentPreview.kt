package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.core.ui.setting.dialog.DialogSettingViewProvider
import net.thunderbird.core.ui.setting.emptySettings
import net.thunderbird.core.validation.input.IntegerInputField
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar

@Composable
@Preview(showBackground = true)
internal fun GeneralSettingsContentPreview() {
    PreviewWithTheme {
        GeneralSettingsContent(
            accountId = AccountIdFactory.create(),
            state = GeneralSettingsContract.State(
                subtitle = "Subtitle",
                name = StringInputField(value = "Alice"),
                color = IntegerInputField(value = 0x112233),
                avatar = Avatar.Monogram(
                    id = AccountIdFactory.create(),
                    value = "AL",
                ),
            ),
            onEvent = {},
            provider = DialogSettingViewProvider(),
            builder = { _, _, _ -> emptySettings() },
        )
    }
}
