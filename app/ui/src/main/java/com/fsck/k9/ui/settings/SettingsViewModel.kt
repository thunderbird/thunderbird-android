package com.fsck.k9.ui.settings

import android.arch.lifecycle.ViewModel
import com.fsck.k9.ui.account.AccountsLiveData

internal class SettingsViewModel(val accounts: AccountsLiveData) : ViewModel()
