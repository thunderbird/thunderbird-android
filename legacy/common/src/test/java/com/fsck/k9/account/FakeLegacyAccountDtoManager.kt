package com.fsck.k9.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager

class FakeLegacyAccountDtoManager(
    private val accounts: MutableMap<String, LegacyAccountDto> = mutableMapOf(),
    private val isFailureOnSave: Boolean = false,
) : LegacyAccountDtoManager {

    override fun getAccounts(): List<LegacyAccountDto> = accounts.values.toList()

    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountUuid: String): LegacyAccountDto? = accounts[accountUuid]

    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto> {
        TODO("Not yet implemented")
    }

    override fun addAccountRemovedListener(listener: AccountRemovedListener) {
        TODO("Not yet implemented")
    }

    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) {
        TODO("Not yet implemented")
    }

    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) {
        TODO("Not yet implemented")
    }

    @Suppress("TooGenericExceptionThrown")
    override fun saveAccount(account: LegacyAccountDto) {
        if (isFailureOnSave) {
            throw Exception("FakeAccountManager.saveAccount() failed")
        }
        accounts[account.uuid] = account
    }
}
