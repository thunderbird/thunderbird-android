package net.thunderbird.app.common.feature.account

import androidx.work.WorkerParameters
import net.thunderbird.app.common.account.data.DefaultLegacyAccountManager
import net.thunderbird.app.common.feature.account.usecase.DefaultDeleteAccount
import net.thunderbird.app.common.feature.account.usecase.DefaultGetDefaultAccountId
import net.thunderbird.feature.account.AccountRepository
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import net.thunderbird.feature.account.usecase.DeleteAccount
import net.thunderbird.feature.account.usecase.GetDefaultAccountId
import org.koin.dsl.module

internal val appCommonFeatureAccountModule = module {
    factory<GetDefaultAccountId> {
        DefaultGetDefaultAccountId(
            accountManager = get(),
        )
    }

    factory<DeleteAccount> {
        DefaultDeleteAccount(
            accountRepository = get(),
        )
    }

    factory<AccountRepository> {
        DefaultAccountRepository(
            accountManager = get<DefaultLegacyAccountManager>(),
        )
    }

    factory {
        AccountRemover(
            localStoreProvider = get(),
            messagingController = get(),
            backendManager = get(),
            localKeyStoreManager = get(),
            accountManager = get(),
            accountRepository = get(),
            unifiedInboxConfigurator = get(),
            avatarImageRepository = get(),
            messageStoreManager = get(),
            logger = get(),
        )
    }
    factory { (parameters: WorkerParameters) ->
        AccountRemoverWorker(
            accountRemover = get(),
            notificationController = get(),
            context = get(),
            workerParams = parameters,
        )
    }
    factory<BackgroundAccountRemover> {
        DefaultBackgroundAccountRemover(
            context = get(),
            accountManager = get(),
        )
    }
}
