package com.fsck.k9.ui

import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FoldersLiveDataFactory
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import com.fsck.k9.ui.helper.HtmlSettingsProvider
import com.fsck.k9.ui.helper.HtmlToSpanned
import org.koin.dsl.module.applicationContext

val uiModule = applicationContext {
    bean { FolderNameFormatter(get()) }
    bean { HtmlToSpanned() }
    bean { ThemeManager(get()) }
    bean { HtmlSettingsProvider(get()) }
    bean { DisplayHtmlUiFactory(get()) }
    bean { FoldersLiveDataFactory(get(), get()) }
}
