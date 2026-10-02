package net.thunderbird.app.common.feature.account.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.app.common.account.data.FakeLegacyAccountManager
import net.thunderbird.app.common.feature.mail.FakeData
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class DefaultGetDefaultAccountIdTest {

    @Test
    fun `return null when no accounts present`() {
        // Arrange
        val accountManager = FakeLegacyAccountManager(emptyList())
        val testSubject = DefaultGetDefaultAccountId(accountManager)

        // Act
        val result = testSubject()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `returns default account id`() {
        // Arrange
        val accountId = AccountIdFactory.create()
        val account = createLegacyAccount(accountId)
        val accountManager = FakeLegacyAccountManager(listOf(account))
        val testSubject = DefaultGetDefaultAccountId(accountManager)

        // Act
        val result = testSubject()

        // Assert
        assertThat(result).isEqualTo(accountId)
    }

    @Test
    fun `returns first account id when multiple accounts exist`() {
        // Arrange
        val firstAccountId = AccountIdFactory.create()
        val secondAccountId = AccountIdFactory.create()
        val firstAccount = createLegacyAccount(firstAccountId)
        val secondAccount = createLegacyAccount(secondAccountId)
        val accountManager = FakeLegacyAccountManager(listOf(firstAccount, secondAccount))
        val testSubject = DefaultGetDefaultAccountId(accountManager)

        // Act
        val result = testSubject()

        // Assert
        assertThat(result).isEqualTo(firstAccountId)
    }

    private companion object {
        fun createLegacyAccount(id: AccountId): LegacyAccount {
            return FakeData.legacyAccount.copy(id = id)
        }
    }
}
