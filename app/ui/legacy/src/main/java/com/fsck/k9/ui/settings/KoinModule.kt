package com.fsck.k9.ui.settings

import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.export.SettingsExportViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.settings.general.GeneralSettingsViewModel
import com.fsck.k9.ui.settings.import.AccountActivator
import com.fsck.k9.ui.settings.import.SettingsImportResultViewModel
import com.fsck.k9.ui.settings.import.SettingsImportViewModel
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
            notificationController = get()
        )
    }

    viewModel { SettingsExportViewModel(context = get(), preferences = get(), settingsExporter = get()) }
    viewModel { SettingsImportViewModel(get(), get()) }
    viewModel { SettingsImportResultViewModel() }

    single { AccountActivator(get(), get(), get()) }
}
