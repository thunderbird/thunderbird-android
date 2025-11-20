package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonSegmentedSingleChoice
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue

@Composable
internal fun <T> CompactSelectSingleOptionItem(
    setting: SettingValue.CompactSelectSingleOption<T>,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MainTheme.spacings.double),
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        TextTitleMedium(text = setting.title())

        ButtonSegmentedSingleChoice(
            onClick = {
                onSettingValueChange(setting.copy(value = it))
            },
            options = setting.options,
            optionTitle = { it.title() },
            modifier = Modifier.fillMaxWidth(),
            selectedOption = setting.value,
        )

        setting.description()?.let {
            TextBodyMedium(
                modifier = Modifier.padding(start = MainTheme.spacings.oneHalf),
                color = MainTheme.colors.onSurfaceVariant,
                text = it,
            )
        }
    }
}
