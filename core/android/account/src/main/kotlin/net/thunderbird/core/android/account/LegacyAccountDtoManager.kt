package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.mail.account.api.AccountManager

@Deprecated(
    message = "Use net.thunderbird.core.android.account.LegacyAccountManager instead",
    replaceWith = ReplaceWith(
        expression = "LegacyAccountManager",
        "net.thunderbird.core.android.account.LegacyAccountManager",
    ),
)
interface LegacyAccountDtoManager : AccountManager<LegacyAccountDto> {
    override fun getAccounts(): List<LegacyAccountDto>
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>>
    override fun getAccount(accountUuid: String): LegacyAccountDto?
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?>
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int)
    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    override fun saveAccount(account: LegacyAccountDto)
}
