package com.fsck.k9.ui.changelog

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val changelogUiModule = module {
    single { ChangeLogManager(context = get(), appCoroutineScope = get(named("AppCoroutineScope"))) }
    viewModel { (mode: ChangeLogMode) ->
        ChangelogViewModel(generalSettingsManager = get(), changeLogManager = get(), mode = mode)
    }
    viewModel { RecentChangesViewModel(generalSettingsManager = get(), changeLogManager = get()) }
}
