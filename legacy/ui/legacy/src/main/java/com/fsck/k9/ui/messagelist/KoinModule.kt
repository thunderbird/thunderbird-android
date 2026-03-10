package com.fsck.k9.ui.messagelist

import com.fsck.k9.ui.messagelist.debug.AuthDebugActions
import net.thunderbird.feature.navigation.drawer.dropdown.navigationDropDownDrawerModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageListUiModule = module {
    includes(navigationDropDownDrawerModule)

    viewModel { MessageListViewModel(messageListLiveDataFactory = get(), logger = get()) }
    factory { DefaultFolderProvider(outboxFolderManager = get()) }
    factory {
        MessageListLoader(
            accountManager = get(),
            localStoreProvider = get(),
            messageListRepository = get(),
            messageHelper = get(),
            messageListPreferencesManager = get(),
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
