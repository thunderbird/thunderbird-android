package com.fsck.k9.ui.settings.autocrypt

import androidx.lifecycle.LiveData
import com.fsck.k9.AccountsChangeListener
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class IdentitiesLiveData(val preferences: Preferences) : LiveData<List<Identity>>(), AccountsChangeListener {

    private fun loadIdentitiesAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            val identities = async {
                loadIdentities()
            }

            value = identities.await()
        }
    }

    override fun onAccountsChanged() {
        loadIdentitiesAsync()
    }

    private fun loadIdentities(): List<Identity> {
        val identities = emptyList<Identity>().toMutableList()
        for (account in preferences.accounts) {
            identities += account.identities
        }
        return identities
    }

    override fun onActive() {
        super.onActive()
        preferences.addOnAccountsChangeListener(this)
        loadIdentitiesAsync()
    }

    override fun onInactive() {
        super.onInactive()
        preferences.removeOnAccountsChangeListener(this)
    }
}
