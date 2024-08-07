package com.fsck.k9.ui.base

import app.k9mail.legacy.ui.theme.ThemeManager
import com.fsck.k9.ui.base.locale.SystemLocaleManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

val uiBaseModule = module {
    single {
        ThemeManager(
            context = get(),
            themeProvider = get(),
            generalSettingsManager = get(),
            appCoroutineScope = get(named("AppCoroutineScope")),
        )
    }
    single { AppLanguageManager(systemLocaleManager = get()) }
    single { SystemLocaleManager(context = get()) }
}
