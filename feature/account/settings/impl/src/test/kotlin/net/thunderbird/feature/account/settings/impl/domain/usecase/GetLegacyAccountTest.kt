package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

internal class GetLegacyAccountTest {

    @Test
    fun `should return success when repository emits account`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()
        val legacyAccount = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) =
                flowOf(legacyAccount)

            override suspend fun update(account: LegacyAccount) {
                error("Not implemented")
            }
        }

        val testSubject = GetLegacyAccount(repository)

        // Act
        val result = testSubject(accountId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)

        val success = result as Outcome.Success
        assertThat(success.data).isEqualTo(legacyAccount)
    }

    @Test
    fun `should return NotFound when repository emits nothing`() = runTest {
        // Arrange
        val accountId = AccountIdFactory.create()

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId): Flow<LegacyAccount?> = emptyFlow()

            override suspend fun update(account: LegacyAccount) {
                error("Not implemented")
            }
        }

        val testSubject = GetLegacyAccount(repository)

        // Act
        val result = testSubject(accountId)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Failure::class)

        val failure = result as Outcome.Failure
        assertThat(failure.error)
            .isInstanceOf(AccountSettingsDomainContract.AccountSettingError.NotFound::class)
    }

    private companion object Companion {
        fun createLegacyAccount(
            id: AccountId,
            displayName: String = "AccountProfileName",
            color: Int = 0xFF0000,
        ): LegacyAccount {
            return LegacyAccount(
                isSensitiveDebugLoggingEnabled = { true },
                id = id,
                name = displayName,
                email = "demo@example.com",
                profile = ProfileDto(
                    id = id,
                    name = displayName,
                    color = color,
                    avatar = AvatarDto(
                        avatarType = AvatarTypeDto.ICON,
                        avatarMonogram = null,
                        avatarImageUri = null,
                        avatarIconName = "star",
                    ),
                ),
                identities = listOf(
                    Identity(
                        signatureUse = false,
                        description = "Demo User",
                    ),
                ),
                incomingServerSettings = ServerSettings(
                    type = "imap",
                    host = "imap.example.com",
                    port = 993,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "test",
                    password = "password",
                    clientCertificateAlias = null,
                ),
                outgoingServerSettings = ServerSettings(
                    type = "smtp",
                    host = "smtp.example.com",
                    port = 465,
                    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                    authenticationType = AuthType.PLAIN,
                    username = "test",
                    password = "password",
                    clientCertificateAlias = null,
                ),
            )
        }
    }
}
