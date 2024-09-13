package app.k9mail.feature.navigation.drawer

import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayAccounts
import app.k9mail.feature.navigation.drawer.domain.usecase.GetDisplayFoldersForAccount
import app.k9mail.feature.navigation.drawer.legacy.AccountsViewModel
import app.k9mail.feature.navigation.drawer.legacy.FoldersViewModel
import app.k9mail.feature.navigation.drawer.ui.DrawerViewModel
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val navigationDrawerModule: Module = module {

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
            getDisplayAccounts = get(),
            getDisplayFoldersForAccount = get(),
        )
    }
}
