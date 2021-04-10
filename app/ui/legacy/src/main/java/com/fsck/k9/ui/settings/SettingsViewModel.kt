package com.fsck.k9.ui.settings

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.ui.account.AccountsLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val accountManager: AccountManager,
    private val coroutineScope: CoroutineScope = GlobalScope,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val accounts: AccountsLiveData
) : ViewModel() {

    fun moveAccount(account: Account, newPosition: Int) {
        coroutineScope.launch(coroutineDispatcher) {
            // Delay saving the account so the animation is not disturbed
            delay(500)

            accountManager.moveAccount(account, newPosition)
        }
    }
}
