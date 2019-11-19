package com.fsck.k9.ui

import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersLiveDataFactory
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import com.fsck.k9.ui.helper.HtmlSettingsProvider
import com.fsck.k9.ui.helper.HtmlToSpanned
import org.koin.dsl.module

val uiModule = module {
    single { FolderNameFormatter(get()) }
    single { HtmlToSpanned() }
    single { ThemeManager(get()) }
    single { HtmlSettingsProvider(get()) }
    single { DisplayHtmlUiFactory(get()) }
    single { FoldersLiveDataFactory(get(), get()) }
}
