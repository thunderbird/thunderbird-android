package com.fsck.k9.ui

import android.content.Context
import com.fsck.k9.ui.helper.DisplayHtmlUiFactory
import com.fsck.k9.ui.helper.HtmlSettingsProvider
import com.fsck.k9.ui.helper.HtmlToSpanned
import com.fsck.k9.ui.helper.SizeFormatter
import org.koin.dsl.module

val uiModule = module {
    single { HtmlToSpanned() }
    single { ThemeManager(get()) }
    single { HtmlSettingsProvider(get()) }
    single { DisplayHtmlUiFactory(get()) }
    factory { (context: Context) -> SizeFormatter(context.resources) }
}
