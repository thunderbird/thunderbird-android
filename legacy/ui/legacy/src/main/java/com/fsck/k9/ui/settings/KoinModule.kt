package com.fsck.k9.ui.settings

import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.account.getSystemVibrator
import com.fsck.k9.ui.settings.export.SettingsExportViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.settings.general.GeneralSettingsViewModel
import java.util.concurrent.Executors
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsUiModule = module {
    viewModel { SettingsViewModel(accountManager = get()) }

    viewModel { GeneralSettingsViewModel(logFileWriter = get()) }
    factory { GeneralSettingsDataStore(jobManager = get(), appLanguageManager = get(), generalSettingsManager = get()) }
    single(named("SaveSettingsExecutorService")) {
        Executors.newSingleThreadExecutor(NamedThreadFactory("SaveSettings"))
    }

    viewModel { AccountSettingsViewModel(get(), get(), get()) }
    single {
        AccountSettingsDataStoreFactory(
            preferences = get(),
            jobManager = get(),
            executorService = get(named("SaveSettingsExecutorService")),
            notificationChannelManager = get(),
            notificationController = get(),
            messagingController = get(),
        )
    }
    factory { getSystemVibrator(context = get()) }

    viewModel {
        SettingsExportViewModel(
            context = get(),
            accountManager = get(),
            settingsExporter = get(),
        )
    }
}
