package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.mail.account.api.AccountManager

@Deprecated(
    message = "Use net.thunderbird.feature.mail.account.api.AccountManager<TAccount : BaseAccount> instead",
    replaceWith = ReplaceWith(
        expression = "AccountManager<LegacyAccount>",
        "net.thunderbird.feature.mail.account.api.AccountManager",
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
