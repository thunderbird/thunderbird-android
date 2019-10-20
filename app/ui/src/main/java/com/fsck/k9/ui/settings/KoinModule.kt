package com.fsck.k9.ui.settings

import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.account.AccountsLiveData
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.export.SettingsExportViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.settings.import.AccountActivator
import com.fsck.k9.ui.settings.import.SettingsImportResultViewModel
import com.fsck.k9.ui.settings.import.SettingsImportViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext
import java.util.concurrent.Executors

val settingsUiModule = applicationContext {
    bean { AccountsLiveData(get()) }
    viewModel { SettingsViewModel(get()) }

    bean { GeneralSettingsDataStore(get(), get(), get("SaveSettingsExecutorService"), get()) }
    bean("SaveSettingsExecutorService") {
        Executors.newSingleThreadExecutor(NamedThreadFactory("SaveSettings"))
    }

    viewModel { AccountSettingsViewModel(get(), get()) }
    bean { AccountSettingsDataStoreFactory(get(), get(), get("SaveSettingsExecutorService")) }

    viewModel { SettingsExportViewModel(get(), get()) }
    viewModel { SettingsImportViewModel(get(), get()) }
    viewModel { SettingsImportResultViewModel() }

    bean { AccountActivator(get(), get(), get(), get()) }
}
