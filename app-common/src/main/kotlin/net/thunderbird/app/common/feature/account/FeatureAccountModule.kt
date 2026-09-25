package net.thunderbird.app.common.feature.account

import androidx.work.WorkerParameters
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover
import org.koin.dsl.module

val featureAccountModule = module {
    factory {
        AccountRemover(
            localStoreProvider = get(),
            messagingController = get(),
            backendManager = get(),
            localKeyStoreManager = get(),
            preferences = get(),
            unifiedInboxConfigurator = get(),
            avatarImageRepository = get(),
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
