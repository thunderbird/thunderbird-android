package com.fsck.k9.ui.base

import com.fsck.k9.ui.base.locale.SystemLocaleManager
import net.thunderbird.core.ui.theme.manager.ThemeManager
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import net.thunderbird.core.ui.theme.api.ThemeManager as ThemeManagerApi

val uiBaseModule = module {
    single {
        ThemeManager(
            context = get(),
            themeProvider = get(),
            generalSettingsManager = get(),
            appCoroutineScope = get(named("AppCoroutineScope")),
        )
    } bind ThemeManagerApi::class
    single { AppLanguageManager(systemLocaleManager = get(), displayCoreSettingsPreferenceManager = get()) }
    single { SystemLocaleManager(context = get()) }
}
