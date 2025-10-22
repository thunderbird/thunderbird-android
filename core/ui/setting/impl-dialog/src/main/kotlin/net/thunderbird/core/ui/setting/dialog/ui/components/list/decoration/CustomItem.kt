package net.thunderbird.core.ui.setting.dialog.ui.components.list.decoration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.SettingDecoration

@Composable
internal fun CustomItem(
    setting: SettingDecoration.Custom,
    modifier: Modifier = Modifier,
) {
    setting.customUi(modifier)
}
