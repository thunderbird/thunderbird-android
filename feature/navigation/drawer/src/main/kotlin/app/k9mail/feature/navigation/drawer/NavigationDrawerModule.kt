package app.k9mail.feature.navigation.drawer

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayAccounts
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayFoldersForAccount
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDrawerConfig
import app.k9mail.feature.navigation.drawer.domain.usecase.SyncAccount
import app.k9mail.feature.navigation.drawer.domain.usecase.SyncAllAccounts
import app.k9mail.feature.navigation.drawer.ui.DrawerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val navigationDrawerModule: Module = module {

    single<UseCase.GetDrawerConfig> {
        GetDrawerConfig(
            configProver = get(),
            generalSettingsManager = get(),
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
            repository = get(),
            messageCountsProvider = get(),
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
            getDisplayAccounts = get(),
            getDisplayFoldersForAccount = get(),
            syncAccount = get(),
            syncAllAccounts = get(),
        )
    }
}
