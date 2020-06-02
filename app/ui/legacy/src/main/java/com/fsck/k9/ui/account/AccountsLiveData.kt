package com.fsck.k9.ui.account

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.AccountsChangeListener
import com.fsck.k9.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountsLiveData(val preferences: Preferences) : LiveData<List<Account>>(), AccountsChangeListener {

    private fun loadAccountsAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            value = withContext(Dispatchers.IO) {
                loadAccounts()
            }
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
