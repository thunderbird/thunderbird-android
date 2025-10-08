package net.thunderbird.feature.account.settings.impl.ui.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.core.ui.setting.Settings

internal class FakeSettingViewProvider : SettingViewProvider {
    @Composable
    override fun SettingView(
        title: String,
        settings: Settings,
        onSettingValueChange: (SettingValue<*>) -> Unit,
        onBack: () -> Unit,
        modifier: Modifier,
        subtitle: String?,
    ) {
        // No-op
    }
}
