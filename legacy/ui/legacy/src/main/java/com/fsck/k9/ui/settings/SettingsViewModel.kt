package com.fsck.k9.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount

internal class SettingsViewModel(
    private val accountManager: AccountManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val accounts = accountManager.getAccountsFlow().asLiveData()

    fun moveAccount(account: LegacyAccount, newPosition: Int) {
        viewModelScope.launch(coroutineDispatcher) {
            // Delay saving the account so the animation is not disturbed
            delay(500)

            withContext(NonCancellable) {
                accountManager.moveAccount(account, newPosition)
            }
        }
    }
}
