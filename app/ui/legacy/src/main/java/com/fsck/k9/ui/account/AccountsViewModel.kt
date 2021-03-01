package com.fsck.k9.ui.account

import androidx.lifecycle.ViewModel
import com.fsck.k9.Preferences

class AccountsViewModel(preferences: Preferences) : ViewModel() {
    val accountsLiveData = AccountsLiveData(preferences)
}
