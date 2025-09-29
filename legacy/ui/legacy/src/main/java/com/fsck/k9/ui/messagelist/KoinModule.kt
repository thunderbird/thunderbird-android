package com.fsck.k9.ui.messagelist

import net.thunderbird.feature.navigation.drawer.dropdown.navigationDropDownDrawerModule
import net.thunderbird.feature.navigation.drawer.siderail.navigationSideRailDrawerModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageListUiModule = module {
    includes(navigationDropDownDrawerModule, navigationSideRailDrawerModule)

    viewModel { MessageListViewModel(messageListLiveDataFactory = get(), logger = get()) }
    factory { DefaultFolderProvider(outboxFolderManager = get()) }
    factory {
        MessageListLoader(
            accountManager = get(),
            localStoreProvider = get(),
            messageListRepository = get(),
            messageHelper = get(),
            generalSettingsManager = get(),
            outboxFolderManager = get(),
        )
    }
    factory {
        MessageListLiveDataFactory(
            messageListLoader = get(),
            accountManager = get(),
            messageListRepository = get(),
        )
    }
    single { SortTypeToastProvider() }
}
