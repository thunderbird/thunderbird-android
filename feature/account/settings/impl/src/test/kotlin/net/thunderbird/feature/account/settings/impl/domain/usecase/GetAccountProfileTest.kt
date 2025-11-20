package net.thunderbird.feature.account.settings.impl.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class GetAccountProfileTest {

    @Test
    fun `should emit profile when account profile present`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val accountProfile = AccountProfile(
            id = accountId,
            name = "Test Account",
            color = 0xFF0000,
            avatar = Avatar.Icon(name = "star"),
        )
        val testSubject = createTestSubject(accountProfile)

        // Act & Assert
        testSubject(accountId).test {
            val outcome = awaitItem()
            assertThat(outcome).isInstanceOf(Outcome.Success::class)

            val success = outcome as Outcome.Success
            val profile = success.data

            assertThat(profile).isEqualTo(accountProfile)
        }
    }

    @Test
    fun `should emit NotFound when account profile not found`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val testSubject = createTestSubject()

        // Act & Assert
        testSubject(accountId).test {
            assertThat(awaitItem()).isEqualTo(
                Outcome.failure(
                    AccountSettingError.NotFound(
                        message = "AccountProfile not found for accountId: ${accountId.asRaw()}",
                    ),
                ),
            )
        }
    }

    private fun createTestSubject(
        accountProfile: AccountProfile? = null,
    ): UseCase.GetAccountProfile {
        return GetAccountProfile(
            repository = FakeAccountProfileRepository(accountProfile),
        )
    }
}
