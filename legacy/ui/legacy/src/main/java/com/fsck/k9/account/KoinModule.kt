package com.fsck.k9.account

import androidx.work.WorkerParameters
import org.koin.dsl.module

val accountModule = module {
    factory {
        AccountRemover(
            localStoreProvider = get(),
            messagingController = get(),
            backendManager = get(),
            localKeyStoreManager = get(),
            preferences = get(),
            unifiedInboxConfigurator = get(),
        )
    }
    factory { BackgroundAccountRemover(get()) }
    factory { (parameters: WorkerParameters) ->
        AccountRemoverWorker(accountRemover = get(), notificationController = get(), context = get(), parameters)
    }
}
