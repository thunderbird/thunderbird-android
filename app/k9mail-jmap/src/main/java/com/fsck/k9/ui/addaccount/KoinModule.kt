package com.fsck.k9.ui.addaccount

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiAddAccountModule = module {
    viewModel { AddAccountViewModel(get(), get(), get()) }
}
