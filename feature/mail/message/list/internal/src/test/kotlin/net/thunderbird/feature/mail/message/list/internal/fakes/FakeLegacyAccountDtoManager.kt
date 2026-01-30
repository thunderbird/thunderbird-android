package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager

internal open class FakeLegacyAccountDtoManager(
    private val accounts: List<LegacyAccountDto>,
) : LegacyAccountDtoManager {
    override fun getAccounts(): List<LegacyAccountDto> = accounts
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> = flowOf(accounts)
    override fun getAccount(accountUuid: String): LegacyAccountDto? = accounts.firstOrNull { it.uuid == accountUuid }
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> = flowOf(getAccount(accountUuid))
    override fun addAccountRemovedListener(listener: AccountRemovedListener) = Unit
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) = Unit
    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun saveAccount(account: LegacyAccountDto) = Unit
}
