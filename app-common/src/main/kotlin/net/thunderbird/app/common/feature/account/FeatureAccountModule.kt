package net.thunderbird.app.common.feature.account

import androidx.work.WorkerParameters
import net.thunderbird.app.common.feature.account.usecase.DefaultGetDefaultAccountId
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import net.thunderbird.feature.account.usecase.GetDefaultAccountId
import org.koin.dsl.module

internal val appCommonFeatureAccountModule = module {
    factory<GetDefaultAccountId> {
        DefaultGetDefaultAccountId(
            accountManager = get(),
        )
    }

    factory {
        AccountRemover(
            localStoreProvider = get(),
            messagingController = get(),
            backendManager = get(),
            localKeyStoreManager = get(),
            accountManager = get(),
            preferences = get(),
            unifiedInboxConfigurator = get(),
            avatarImageRepository = get(),
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
        DefaultBackgroundAccountRemover(get())
    }
}
