package com.fsck.k9.ui.choosefolder

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val chooseFolderUiDiModule = module {
    viewModel { ChooseFolderViewModel(get()) }
}
