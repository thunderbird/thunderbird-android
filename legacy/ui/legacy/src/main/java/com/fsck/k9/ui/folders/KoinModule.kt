package com.fsck.k9.ui.folders

import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.K9
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val foldersUiModule = module {
    factory { FolderNameFormatter(resources = get()) }
    viewModel {
        FoldersViewModel(
            folderRepository = get(),
            messageCountsProvider = get(),
            isShowUnifiedInbox = { K9.isShowUnifiedInbox },
        )
    }
    factory { FolderIconProvider() }
}
