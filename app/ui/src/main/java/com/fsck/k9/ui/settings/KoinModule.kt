package com.fsck.k9.ui.settings

import com.fsck.k9.helper.NamedThreadFactory
import com.fsck.k9.ui.account.AccountsLiveData
import com.fsck.k9.ui.settings.account.AccountSettingsDataStoreFactory
import com.fsck.k9.ui.settings.account.AccountSettingsViewModel
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import java.util.concurrent.Executors

val settingsUiModule = module {
    single { AccountsLiveData(get()) }
    viewModel { SettingsViewModel(get()) }

    single { GeneralSettingsDataStore(get(), get(), get("SaveSettingsExecutorService")) }
    single("SaveSettingsExecutorService") {
        Executors.newSingleThreadExecutor(NamedThreadFactory("SaveSettings"))
    }

    viewModel { AccountSettingsViewModel(get(), get()) }
    single { AccountSettingsDataStoreFactory(get(), get(), get("SaveSettingsExecutorService")) }
}
