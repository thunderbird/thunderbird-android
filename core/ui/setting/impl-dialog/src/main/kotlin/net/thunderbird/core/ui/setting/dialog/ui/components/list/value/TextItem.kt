package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.list.SettingItemLayout

@Composable
internal fun TextItem(
    setting: SettingValue.Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = onClick,
        icon = setting.icon(),
        modifier = modifier,
    ) {
        TextTitleMedium(text = setting.title())
        TextBodyMedium(text = setting.value)
    }
}
