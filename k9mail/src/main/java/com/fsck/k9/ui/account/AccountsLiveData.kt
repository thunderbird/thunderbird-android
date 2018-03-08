package com.fsck.k9.ui.account

import android.arch.lifecycle.LiveData
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

class AccountsLiveData(context: Context) : LiveData<List<Account>>() {
    init {
        loadAccountsAsync(context)
    }

    private fun loadAccountsAsync(context: Context) {
        launch(UI) {
            val accounts = bg {
                loadAccounts(context)
            }

            value = accounts.await()
        }
    }

    private fun loadAccounts(context: Context): List<Account> {
        return Preferences.getPreferences(context).accounts
    }
}
