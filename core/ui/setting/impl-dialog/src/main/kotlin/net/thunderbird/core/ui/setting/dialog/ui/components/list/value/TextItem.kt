package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout

private const val DISABLED_ROW_ALPHA = 0.4f

@Composable
internal fun TextItem(
    setting: SettingValue.Text,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingItemLayout(
        onClick = if (enabled) onClick else null,
        icon = setting.icon(),
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ROW_ALPHA),
    ) {
        TextTitleMedium(text = setting.title())
        TextBodyMedium(text = setting.value)
    }
}
