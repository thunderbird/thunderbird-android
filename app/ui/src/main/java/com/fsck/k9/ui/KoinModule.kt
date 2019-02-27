package com.fsck.k9.ui

import com.fsck.k9.ui.folders.FolderNameFormatter
import org.koin.dsl.module.module


val uiModule = module {
    single { FolderNameFormatter(get()) }
}
