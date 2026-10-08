package com.fsck.k9.ui.managefolders

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val manageFoldersUiModule = module {
    viewModel { ManageFoldersViewModel(folderRepository = get()) }
    viewModel {
        FolderSettingsViewModel(
            accountManager = get(),
            folderDetailsRepository = get(),
            messagingController = get(),
        )
    }
}
