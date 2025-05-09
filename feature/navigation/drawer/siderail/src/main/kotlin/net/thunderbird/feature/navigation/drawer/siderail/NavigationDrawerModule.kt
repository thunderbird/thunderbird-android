package net.thunderbird.feature.navigation.drawer.siderail

import net.thunderbird.feature.navigation.drawer.siderail.data.UnifiedFolderRepository
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract
import net.thunderbird.feature.navigation.drawer.siderail.domain.DomainContract.UseCase
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.GetDisplayAccounts
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.GetDisplayFoldersForAccount
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.GetDrawerConfig
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.SaveDrawerConfig
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.SyncAccount
import net.thunderbird.feature.navigation.drawer.siderail.domain.usecase.SyncAllAccounts
import net.thunderbird.feature.navigation.drawer.siderail.ui.DrawerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationSideRailDrawerModule: Module = module {

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
        )
    }

    single<UseCase.GetDisplayFoldersForAccount> {
        GetDisplayFoldersForAccount(
            displayFolderRepository = get(),
            unifiedFolderRepository = get(),
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
            syncAccount = get(),
            syncAllAccounts = get(),
        )
    }
}
