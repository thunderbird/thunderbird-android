package net.thunderbird.core.android.account

import androidx.annotation.Discouraged
import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager

@Discouraged(
    message = "Use net.thunderbird.core.android.account.LegacyAccountManager instead.",
)
interface LegacyAccountDtoManager : AccountManager<LegacyAccountDto> {
    override fun getAccounts(): List<LegacyAccountDto>
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>>
    override fun getById(accountId: AccountId): LegacyAccountDto?
    override fun observeById(accountId: AccountId): Flow<LegacyAccountDto?>
    fun addAccountRemovedListener(listener: AccountRemovedListener)
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int)
    fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener)
    override fun saveAccount(account: LegacyAccountDto)
}
