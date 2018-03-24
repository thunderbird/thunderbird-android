package com.fsck.k9.ui.settings

import com.fsck.k9.ui.account.AccountsLiveData
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext

val settingsUiModule = applicationContext {
    bean { AccountsLiveData(get()) }
    viewModel { SettingsViewModel(get()) }
}
