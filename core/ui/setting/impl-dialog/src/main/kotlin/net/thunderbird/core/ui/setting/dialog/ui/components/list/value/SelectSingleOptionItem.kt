package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.list.SettingItemLayout

@Composable
internal fun SelectSingleOptionItem(
    setting: SettingValue.SelectSingleOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = onClick,
        icon = setting.icon(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
            ) {
                TextTitleMedium(text = setting.value.title())
                setting.description()?.let {
                    TextBodyMedium(text = it)
                }
            }
        }
    }
}
