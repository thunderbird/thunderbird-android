package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue

@Composable
internal fun SwitchItem(
    setting: SettingValue.Switch,
    modifier: Modifier = Modifier,
    onSettingValueChange: (SettingValue<*>) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(MainTheme.spacings.double),
    ) {
        Column(
            Modifier.weight(1f),
        ) {
            TextTitleMedium(text = setting.title())
            setting.description()?.let {
                TextBodyMedium(text = it)
            }
        }
        Switch(
            checked = setting.value,
            onCheckedChange = {
                onSettingValueChange(setting.copy(value = it))
            },
        )
    }
}
