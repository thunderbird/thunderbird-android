package net.thunderbird.core.ui.setting.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.ui.setting.dialog.ui.DialogSettingView

class DialogSettingViewProvider : SettingViewProvider {

    @Composable
    override fun SettingView(
        title: String,
        settings: Settings,
        onSettingValueChange: (SettingValue<*>) -> Unit,
        onBack: () -> Unit,
        modifier: Modifier,
        subtitle: String?,
    ) {
        DialogSettingView(
            title = title,
            settings = settings,
            onSettingValueChange = onSettingValueChange,
            onBack = onBack,
            modifier = modifier,
            subtitle = subtitle,
        )
    }
}
