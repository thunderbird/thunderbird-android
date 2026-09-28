package com.fsck.k9.account

import app.k9mail.feature.account.setup.AccountSetupExternalContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.usecase.GetDefaultAccountId

// TODO move to feature account
class AccountOwnerNameProvider(
    private val accountManager: LegacyAccountManager,
    private val getDefaultAccountId: GetDefaultAccountId,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountSetupExternalContract.AccountOwnerNameProvider {
    override suspend fun getOwnerName(): String? {
        return withContext(coroutineDispatcher) {
            getDefaultAccountId()?.let {
                accountManager.getByIdSync(it)?.senderName
            }
        }
    }
}
