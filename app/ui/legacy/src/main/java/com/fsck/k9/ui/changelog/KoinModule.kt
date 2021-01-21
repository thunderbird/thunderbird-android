package com.fsck.k9.ui.changelog

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val changelogUiModule = module {
    viewModel { ChangelogViewModel(context = get()) }
}
