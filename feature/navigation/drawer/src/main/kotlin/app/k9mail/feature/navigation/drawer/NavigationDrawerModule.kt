package app.k9mail.feature.navigation.drawer

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayAccounts
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayFoldersForAccount
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDrawerConfig
import app.k9mail.feature.navigation.drawer.domain.usecase.SyncAccount
import app.k9mail.feature.navigation.drawer.domain.usecase.SyncAllAccounts
import app.k9mail.feature.navigation.drawer.legacy.AccountsViewModel
import app.k9mail.feature.navigation.drawer.legacy.FoldersViewModel
import app.k9mail.feature.navigation.drawer.ui.DrawerViewModel
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val navigationDrawerModule: Module = module {

    single<UseCase.GetDrawerConfig> {
        GetDrawerConfig(
            configProver = get(),
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
            messagingController = get(),
        )
    }

    single<UseCase.SyncAllAccounts> {
        SyncAllAccounts(
            messagingController = get(),
        )
    }

    viewModel {
        AccountsViewModel(
            getDisplayAccounts = get(),
        )
    }

    viewModel {
        val coreResourceProvider = get<CoreResourceProvider>()

        FoldersViewModel(
            folderRepository = get(),
            messageCountsProvider = get(),
            isShowUnifiedInbox = { K9.isShowUnifiedInbox },
            getUnifiedInboxTitle = { coreResourceProvider.searchUnifiedInboxTitle() },
            getUnifiedInboxDetail = { coreResourceProvider.searchUnifiedInboxDetail() },
        )
    }

    viewModel {
        DrawerViewModel(
            getDrawerConfig = get(),
            getDisplayAccounts = get(),
            getDisplayFoldersForAccount = get(),
            syncAccount = get(),
        )
    }
}
