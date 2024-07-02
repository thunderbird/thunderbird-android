package com.fsck.k9.ui.folders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val foldersUiModule = module {
    factory { FolderNameFormatter(resources = get()) }
    viewModel { FoldersViewModel(folderRepository = get(), messageCountsProvider = get()) }
    factory { FolderIconProvider() }
}
