package com.fsck.k9.ui.account

import android.arch.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.AccountsChangeListener
import com.fsck.k9.Preferences
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

class AccountsLiveData(val preferences: Preferences) : LiveData<List<Account>>(), AccountsChangeListener {

    private fun loadAccountsAsync() {
        launch(UI) {
            val accounts = bg {
                loadAccounts()
            }

            value = accounts.await()
        }
    }

    override fun onAccountsChanged() {
        loadAccountsAsync()
    }

    private fun loadAccounts(): List<Account> {
        return preferences.accounts
    }

    override fun onActive() {
        super.onActive()
        preferences.addOnAccountsChangeListener(this)
        loadAccountsAsync()
    }

    override fun onInactive() {
        super.onInactive()
        preferences.removeOnAccountsChangeListener(this)
    }
}
