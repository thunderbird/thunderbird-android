package net.thunderbird.core.ui.setting.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.core.ui.setting.Settings

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
        TextTitleMedium("This is going to be implemented soon.")
    }
}
