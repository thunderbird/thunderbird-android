package com.fsck.k9.ui.settings

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.fsck.k9.ui.account.AccountsLiveData

internal class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val accounts = AccountsLiveData(application)
}
