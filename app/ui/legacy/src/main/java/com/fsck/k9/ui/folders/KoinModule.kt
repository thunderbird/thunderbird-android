package com.fsck.k9.ui.folders

import android.content.Context
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val foldersUiModule = module {
    single { FolderNameFormatterFactory() }
    factory { (context: Context) -> FolderNameFormatter(context.resources) }
    viewModel { FoldersViewModel(folderRepository = get(), messageCountsProvider = get()) }
}
