package com.fsck.k9.ui.account

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val accountUiModule = module {
    viewModel { AccountsViewModel(preferences = get()) }
}
