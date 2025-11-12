package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.dialog.ui.components.common.AutoHeightLazyVerticalGrid
import net.thunderbird.core.ui.setting.dialog.ui.components.common.IconView
import net.thunderbird.core.ui.setting.dialog.ui.components.list.SettingItemLayout

@Composable
internal fun IconListItem(
    setting: SettingValue.IconList,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItemLayout(
        onClick = null,
        icon = setting.icon(),
        modifier = modifier,
    ) {
        TextTitleMedium(text = setting.title())
        setting.description()?.let {
            TextBodyMedium(text = it)
        }
        Spacer(modifier = Modifier.height(MainTheme.spacings.half))

        AutoHeightLazyVerticalGrid(
            items = setting.icons,
            itemSize = MainTheme.sizes.iconAvatar,
        ) { icon ->
            IconView(
                icon = icon,
                color = setting.color,
                isSelected = icon == setting.value,
                onClick = {
                    onSettingValueChange(setting.copy(value = icon))
                },
                modifier = Modifier.size(MainTheme.sizes.iconAvatar),
            )
        }
    }
}
