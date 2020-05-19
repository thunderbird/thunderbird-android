package com.fsck.k9.ui.base

import org.koin.dsl.module

val uiBaseModule = module {
    single { ThemeManager(context = get(), themeProvider = get()) }
}
