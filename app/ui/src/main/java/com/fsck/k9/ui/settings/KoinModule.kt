package com.fsck.k9.ui.settings

import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.account.AccountsLiveData
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.openpgp.IdentitiesLiveData
import com.fsck.k9.ui.settings.openpgp.SettingsOpenPgpViewModel
import com.fsck.k9.ui.settings.export.SettingsExportViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.settings.import.AccountActivator
import com.fsck.k9.ui.settings.import.SettingsImportResultViewModel
import com.fsck.k9.ui.settings.import.SettingsImportViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.Executors

val settingsUiModule = module {
    single { AccountsLiveData(get()) }
    viewModel { SettingsViewModel(get()) }

    single { GeneralSettingsDataStore(get(), get(), get(named("SaveSettingsExecutorService")), get()) }
    single(named("SaveSettingsExecutorService")) {
        Executors.newSingleThreadExecutor(NamedThreadFactory("SaveSettings"))
    }

    viewModel { AccountSettingsViewModel(get(), get()) }
    single { AccountSettingsDataStoreFactory(get(), get(), get(named("SaveSettingsExecutorService"))) }

    viewModel { SettingsExportViewModel(get(), get()) }
    viewModel { SettingsImportViewModel(get(), get()) }
    viewModel { SettingsImportResultViewModel() }

    bean { IdentitiesLiveData(get()) }
    viewModel { SettingsOpenPgpViewModel(get()) }

    single { AccountActivator(get(), get(), get(), get()) }
}
