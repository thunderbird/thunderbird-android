package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
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

internal class UpdateSearchSettingsTest {

    @Test
    fun `should return NotFound when account does not exist`() = runTest {
        val accountId = AccountIdFactory.create()

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId): Flow<LegacyAccount?> = emptyFlow()
            override suspend fun update(account: LegacyAccount) = Unit
        }

        val testSubject = UpdateSearchSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateSearchSettingsCommand
                .UpdateServerSearchLimit(50),
        )

        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    @Test
    fun `should update remote search limit`() = runTest {
        val accountId = AccountIdFactory.create()

        var updatedAccount: LegacyAccount? = null
        val existing = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) = flowOf(existing)

            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val testSubject = UpdateSearchSettings(repository)

        val newLimit = 100

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateSearchSettingsCommand
                .UpdateServerSearchLimit(newLimit),
        )

        val updated = updatedAccount ?: error("Account was not updated")

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updated).isNotNull()
        assertThat(updated.remoteSearchNumResults)
            .isEqualTo(newLimit)

        // Ensure other fields remain unchanged
        assertThat(updated.name).isEqualTo(existing.name)
        assertThat(updated.email).isEqualTo(existing.email)
    }

    @Test
    fun `should allow updating remote search limit to zero`() = runTest {
        val accountId = AccountIdFactory.create()

        var updatedAccount: LegacyAccount? = null
        val existing = createLegacyAccount(accountId)

        val repository = object : LegacyAccountRepository {
            override fun getById(id: AccountId) = flowOf(existing)

            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val testSubject = UpdateSearchSettings(repository)

        val result = testSubject(
            accountId,
            AccountSettingsDomainContract.UpdateSearchSettingsCommand
                .UpdateServerSearchLimit(0),
        )
        val updated = updatedAccount ?: error("Account was not updated")
        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updated.remoteSearchNumResults)
            .isEqualTo(0)
    }

    private companion object {
        fun createLegacyAccount(
            id: AccountId,
            displayName: String = "AccountProfileName",
            color: Int = 0xFF0000,
        ) = LegacyAccount(
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
            remoteSearchNumResults = 25,
        )
    }
}
