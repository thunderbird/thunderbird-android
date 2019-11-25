package com.fsck.k9.ui.settings.openpgp

import androidx.lifecycle.ViewModel
import com.fsck.k9.Preferences
import com.fsck.k9.ui.account.AccountsLiveData

class OpenPgpSettingsViewModel(preferences: Preferences) : ViewModel() {
    val accounts = AccountsLiveData(preferences)
}
