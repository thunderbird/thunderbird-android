package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout
import net.thunderbird.core.ui.setting.dialog.ui.components.common.ColorView

@Composable
internal fun ColorItem(
    setting: SettingValue.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = onClick,
        icon = setting.icon(),
        trailingContent = {
            ColorView(
                color = setting.value,
                onClick = null,
                modifier = Modifier.padding(start = MainTheme.spacings.default),
            )
        },
        modifier = modifier,
    ) {
        TextTitleMedium(text = setting.title())
        setting.description()?.let {
            TextBodyMedium(text = it)
        }
    }
}
