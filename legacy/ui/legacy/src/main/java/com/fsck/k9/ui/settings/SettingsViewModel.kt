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
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal class SettingsViewModel(
    private val accountManager: LegacyAccountManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val accounts = accountManager.observeAll().asLiveData()

    fun moveAccount(accountId: AccountId, newPosition: Int) {
        viewModelScope.launch(coroutineDispatcher) {
            // Delay saving the account so the animation is not disturbed
            delay(500)

            withContext(NonCancellable) {
                accountManager.moveAccount(accountId, newPosition)
            }
        }
    }
}
