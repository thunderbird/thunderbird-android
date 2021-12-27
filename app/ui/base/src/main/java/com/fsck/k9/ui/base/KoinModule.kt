package com.fsck.k9.ui.base

import org.koin.core.qualifier.named
import org.koin.dsl.module

val uiBaseModule = module {
    single { ThemeManager(context = get(), themeProvider = get(), generalSettingsManager = get(), appCoroutineScope = get(named("AppCoroutineScope"))) }
    single { AppLanguageManager() }
}
