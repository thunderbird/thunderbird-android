package com.fsck.k9.ui.managefolders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val manageFoldersUiModule = module {
    viewModel { ManageFoldersViewModel(foldersLiveDataFactory = get()) }
    viewModel { FolderSettingsViewModel(preferences = get(), folderRepositoryManager = get(), messagingController = get()) }
}
