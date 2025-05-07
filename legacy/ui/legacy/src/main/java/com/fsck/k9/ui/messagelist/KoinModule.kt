package com.fsck.k9.ui.messagelist

import net.thunderbird.feature.navigation.drawer.dropdown.navigationDropDownDrawerModule
import net.thunderbird.feature.navigation.drawer.siderail.navigationSideRailDrawerModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageListUiModule = module {
    includes(navigationDropDownDrawerModule, navigationSideRailDrawerModule)

    viewModel { MessageListViewModel(get()) }
    factory { DefaultFolderProvider() }
    factory {
        MessageListLoader(
            preferences = get(),
            localStoreProvider = get(),
            messageListRepository = get(),
            messageHelper = get(),
        )
    }
    factory {
        MessageListLiveDataFactory(messageListLoader = get(), preferences = get(), messageListRepository = get())
    }
    single { SortTypeToastProvider() }
}
