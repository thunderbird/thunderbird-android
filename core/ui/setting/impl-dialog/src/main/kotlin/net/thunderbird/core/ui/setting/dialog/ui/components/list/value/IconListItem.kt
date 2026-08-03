package net.thunderbird.core.ui.setting.dialog.ui.components.list.value

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.component.list.item.SettingItemLayout
import net.thunderbird.core.ui.setting.dialog.ui.components.common.AutoHeightLazyVerticalGrid
import net.thunderbird.core.ui.setting.dialog.ui.components.common.IconView

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
        Spacer(modifier = Modifier.height(BoltTheme.spacings.half))

        AutoHeightLazyVerticalGrid(
            items = setting.icons,
            itemSize = BoltTheme.sizes.iconAvatar,
        ) { icon ->
            IconView(
                icon = icon,
                color = setting.color,
                isSelected = icon == setting.value,
                onClick = {
                    onSettingValueChange(setting.copy(value = icon))
                },
                modifier = Modifier.size(BoltTheme.sizes.iconAvatar),
            )
        }
    }
}
