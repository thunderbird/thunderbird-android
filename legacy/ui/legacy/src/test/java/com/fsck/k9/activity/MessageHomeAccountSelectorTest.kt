package com.fsck.k9.activity

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test

class MessageHomeAccountSelectorTest {
    private val firstAccount = LegacyAccountDto("11111111-1111-1111-1111-111111111111")
    private val secondAccount = LegacyAccountDto("22222222-2222-2222-2222-222222222222")
    private val accountManager = FakeLegacyAccountDtoManager(firstAccount, secondAccount)

    @Test
    fun `single-account search should replace current account`() {
        val search = LocalMessageSearch().apply {
            addAccountUuid(secondAccount.uuid)
        }

        val account = search.resolveAccount(
            currentAccount = firstAccount,
            accountManager = accountManager,
        )

        assertThat(account).isSameInstanceAs(secondAccount)
    }

    private class FakeLegacyAccountDtoManager(
        vararg accounts: LegacyAccountDto,
    ) : LegacyAccountDtoManager {
        private val accounts = accounts.associateBy { it.uuid }

        override fun getAccounts(): List<LegacyAccountDto> = accounts.values.toList()

        override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> = error("Not implemented")

        override fun getAccount(accountUuid: String): LegacyAccountDto? = accounts[accountUuid]

        override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> = error("Not implemented")

        override fun addAccountRemovedListener(listener: AccountRemovedListener) = error("Not implemented")

        override fun moveAccount(account: LegacyAccountDto, newPosition: Int) = error("Not implemented")

        override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) =
            error("Not implemented")

        override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) =
            error("Not implemented")

        override fun saveAccount(account: LegacyAccountDto) = error("Not implemented")
    }
}
