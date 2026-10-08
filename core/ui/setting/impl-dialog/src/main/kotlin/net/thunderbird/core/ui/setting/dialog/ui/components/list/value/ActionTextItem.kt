package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout

@Composable
internal fun ActionTextItem(
    setting: SettingValue.ActionText,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = setting.onClick,
        icon = setting.icon(),
        modifier = modifier,
    ) {
        TextTitleMedium(text = setting.title())
        TextBodyMedium(text = setting.value)
    }
}
