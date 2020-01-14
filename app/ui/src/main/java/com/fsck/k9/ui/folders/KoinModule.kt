package com.fsck.k9.ui.folders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val foldersUiModule = module {
    single { FolderNameFormatter(get()) }
    single { FoldersLiveDataFactory(get(), get(), get()) }
    viewModel { FoldersViewModel(get()) }
}
