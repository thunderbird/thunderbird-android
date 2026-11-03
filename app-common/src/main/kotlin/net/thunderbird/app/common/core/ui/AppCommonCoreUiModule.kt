package net.thunderbird.app.common.core.ui

import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.core.ui.setting.dialog.DialogSettingViewProvider
import org.koin.dsl.module

val appCommonCoreUiModule = module {
    single<SettingViewProvider> {
        val preferences = get<InteractionSettingsPreferenceManager>()
        DialogSettingViewProvider(isKeypadNavigationEnabled = { preferences.getConfig().isKeypadNavigationEnabled })
    }
}
