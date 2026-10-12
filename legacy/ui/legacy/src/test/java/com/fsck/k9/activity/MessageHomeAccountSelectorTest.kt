package com.fsck.k9.activity

import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.FakeLegacyAccount
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import org.junit.Test

class MessageHomeAccountSelectorTest {
    private val firstAccount = FakeLegacyAccount.create(AccountIdFactory.create())
    private val secondAccount = FakeLegacyAccount.create(AccountIdFactory.create())
    private val accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount))

    @Test
    fun `single-account search should replace current account`() {
        val search = LocalMessageSearch().apply {
            addAccountId(secondAccount.id)
        }

        val account = search.resolveAccount(
            currentAccount = firstAccount,
            accountManager = accountManager,
        )

        assertThat(account).isSameInstanceAs(secondAccount)
    }
}
