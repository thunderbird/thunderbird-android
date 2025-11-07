package net.thunderbird.feature.navigation.drawer.dropdown

import net.thunderbird.feature.navigation.drawer.dropdown.data.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.dropdown.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.GetDisplayAccounts
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.GetDisplayFoldersForAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.GetDisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.GetDrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.SaveDrawerConfig
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.SyncAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase.SyncAllAccounts
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationDropDownDrawerModule: Module = module {

    single<DomainContract.UnifiedFolderRepository> {
        UnifiedFolderRepository(
            messageCountsProvider = get(),
        )
    }

    single<UseCase.GetDrawerConfig> {
        GetDrawerConfig(
            configLoader = get(),
        )
    }
    single<UseCase.SaveDrawerConfig> {
        SaveDrawerConfig(
            drawerConfigWriter = get(),
        )
    }

    single<UseCase.GetDisplayAccounts> {
        GetDisplayAccounts(
            accountManager = get(),
            messageCountsProvider = get(),
            messageListRepository = get(),
            notificationStream = get(),
            featureFlagProvider = get(),
            avatarMapper = get(),
        )
    }

    single<UseCase.GetDisplayFoldersForAccount> {
        GetDisplayFoldersForAccount(
            displayFolderRepository = get(),
            unifiedFolderRepository = get(),
        )
    }

    single<UseCase.GetDisplayTreeFolder> {
        GetDisplayTreeFolder(
            logger = get(),
        )
    }

    single<UseCase.SyncAccount> {
        SyncAccount(
            accountManager = get(),
            messagingController = get(),
        )
    }

    single<UseCase.SyncAllAccounts> {
        SyncAllAccounts(
            messagingController = get(),
        )
    }

    viewModel {
        DrawerViewModel(
            getDrawerConfig = get(),
            saveDrawerConfig = get(),
            getDisplayAccounts = get(),
            getDisplayFoldersForAccount = get(),
            getDisplayTreeFolder = get(),
            syncAccount = get(),
            syncAllAccounts = get(),
        )
    }
}
