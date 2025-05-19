package net.thunderbird.feature.account.settings.impl.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

class GetAccountNameTest {

    @Test
    fun `should emit account name when account profile present`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.new()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
        )
        val testSubject = createTestSubject(accountProfile)

        // Act & Assert
        testSubject(accountId).test {
            val outcome = awaitItem()
            assertThat(outcome).isInstanceOf(Outcome.Success::class)

            val success = outcome as Outcome.Success
            assertThat(success.data).isEqualTo(accountProfile.name)
        }
    }

    @Test
    fun `should emit NotFound when account profile not present`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.new()
        val testSubject = createTestSubject()

        // Act & Assert
        testSubject(accountId).test {
            val outcome = awaitItem()
            assertThat(outcome).isInstanceOf(Outcome.Failure::class)

            val failure = outcome as Outcome.Failure
            assertThat(failure.error).isInstanceOf(SettingsError.NotFound::class)
        }
    }

    private fun createTestSubject(
        accountProfile: AccountProfile? = null,
    ): UseCase.GetAccountName {
        return GetAccountName(
            repository = FakeAccountProfileRepository(accountProfile),
        )
    }
}
