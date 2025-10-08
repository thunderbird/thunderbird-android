package net.thunderbird.core.ui.setting.dialog.ui.components.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings

@Composable
internal fun SettingList(
    settings: Settings,
    onItemClick: (index: Int, item: Setting) -> Unit,
    onSettingValueChange: (SettingValue<*>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        itemsIndexed(settings) { index, setting ->
            SettingItem(
                setting = setting,
                onClick = {
                    onItemClick(index, setting)
                },
                onSettingValueChange = onSettingValueChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
