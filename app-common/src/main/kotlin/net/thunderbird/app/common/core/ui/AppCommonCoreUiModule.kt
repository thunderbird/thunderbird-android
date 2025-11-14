package net.thunderbird.app.common.core.ui

import net.thunderbird.core.ui.setting.SettingViewProvider
import net.thunderbird.core.ui.setting.dialog.DialogSettingViewProvider
import org.koin.dsl.module

val appCommonCoreUiModule = module {
    single<SettingViewProvider> { DialogSettingViewProvider() }
}
