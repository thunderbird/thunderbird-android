package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.setting.SettingValue

@Composable
internal fun SwitchItem(
    setting: SettingValue.Switch,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(BoltTheme.spacings.double),
    ) {
        Column(
            Modifier.weight(1f),
        ) {
            TextTitleMedium(text = setting.title())
            setting.description()?.let {
                TextBodyMedium(text = it)
            }
        }
        Spacer(modifier = Modifier.width(BoltTheme.spacings.default))
        Switch(
            checked = setting.value,
            onCheckedChange = {
                onSettingValueChange(setting.copy(value = it))
            },
        )
    }
}
