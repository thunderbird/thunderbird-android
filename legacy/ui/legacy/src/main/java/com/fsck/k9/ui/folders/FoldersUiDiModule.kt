package com.fsck.k9.ui.folders

import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import org.koin.dsl.module

internal val foldersUiDiModule = module {
    factory { FolderNameFormatter(resources = get()) }
    factory { FolderIconProvider() }
}
