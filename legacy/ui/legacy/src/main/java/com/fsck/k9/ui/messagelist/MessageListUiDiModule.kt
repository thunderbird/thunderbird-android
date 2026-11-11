package com.fsck.k9.ui.messagelist

import com.fsck.k9.ui.messagelist.debug.AuthDebugActions
import net.thunderbird.feature.navigation.drawer.dropdown.featureNavigationDrawerDropdownDiModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageListUiDiModule = module {
    includes(featureNavigationDrawerDropdownDiModule)

    viewModel { MessageListViewModel(messageListLiveDataFactory = get(), logger = get()) }
    factory {
        DefaultFolderProvider(
            accountManager = get(),
            outboxFolderManager = get(),
        )
    }
    factory {
        MessageListLoader(
            accountManager = get(),
            localStoreProvider = get(),
            messageListRepository = get(),
            messageHelper = get(),
            messageListPreferencesManager = get(),
            outboxFolderManager = get(),
            featureFlagProvider = get(),
            contactLetterBitmapCreator = get(),
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
