package com.fsck.k9.ui.messagelist

import com.fsck.k9.ui.messagelist.debug.AuthDebugActions
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
    factory {
        AuthDebugActions(
            accountManager = get(),
            oAuth2TokenProviderFactory = get(),
        )
    }
    single { SortTypeToastProvider() }
}
