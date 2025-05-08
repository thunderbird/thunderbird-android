package app.k9mail.legacy.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.account.AccountManager

@Deprecated(
    message = "Use net.thunderbird.core.account.AccountManager<TAccount : BaseAccount> instead",
    replaceWith = ReplaceWith(
        expression = "AccountManager<LegacyAccount>",
        "net.thunderbird.core.account.AccountManager",
        "app.k9mail.legacy.account.LegacyAccount",
    ),
)
interface AccountManager : AccountManager<LegacyAccount> {
    override fun getAccounts(): List<LegacyAccount>
    override fun getAccountsFlow(): Flow<List<LegacyAccount>>
    override fun getAccount(accountUuid: String): LegacyAccount?
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?>
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    override fun moveAccount(account: LegacyAccount, newPosition: Int)
    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    override fun saveAccount(account: LegacyAccount)
}
