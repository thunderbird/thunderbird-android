package com.fsck.k9.ui.account

import android.arch.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

class AccountsLiveData(val preferences: Preferences) : LiveData<List<Account>>() {
    init {
        loadAccountsAsync()
    }

    private fun loadAccountsAsync() {
        launch(UI) {
            val accounts = bg {
                loadAccounts()
            }

            value = accounts.await()
        }
    }

    private fun loadAccounts(): List<Account> {
        return preferences.accounts
    }
}
