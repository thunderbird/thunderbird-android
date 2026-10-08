package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.button.ButtonSegmentedSingleChoice
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout

@Composable
internal fun <T> SegmentedButtonItem(
    setting: SettingValue.SegmentedButton<T>,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = null,
        icon = null,
        modifier = modifier,
    ) {
        TextTitleMedium(text = setting.title())
        setting.description()?.let {
            TextBodyMedium(text = it)
        }

        ButtonSegmentedSingleChoice(
            onClick = {
                onSettingValueChange(setting.copy(value = it))
            },
            options = setting.options,
            optionTitle = { it.title() },
            modifier = Modifier.fillMaxWidth(),
            selectedOption = setting.value,
        )
    }
}
