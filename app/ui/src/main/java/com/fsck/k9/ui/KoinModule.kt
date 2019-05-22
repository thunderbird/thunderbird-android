package com.fsck.k9.ui

import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.helper.HtmlToSpanned
import org.koin.dsl.module.applicationContext

val uiModule = applicationContext {
    bean { FolderNameFormatter(get()) }
    bean { HtmlToSpanned() }
}
