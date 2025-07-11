package com.fsck.k9.ui.choosefolder

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chooseFolderUiModule = module {
    viewModel { ChooseFolderViewModel(get()) }
}
