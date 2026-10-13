package net.thunderbird.feature.account.settings.impl.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

internal class UpdateApiClientIdTest {

    @Test
    fun `saves trimmed Client ID for the selected Gmail account and preserves existing extras`() = runTest {
        val accountId = AccountIdFactory.create()
        val existing = createAccount(
            accountId = accountId,
            serverSettings = gmailSettings(
                extra = mapOf("unrelated" to "preserved"),
            ),
        )
        var updatedAccount: LegacyAccount? = null
        val repository = object : LegacyAccountRepository {
            override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = flowOf(existing)
            override suspend fun update(account: LegacyAccount) {
                updatedAccount = account
            }
        }

        val result = UpdateApiClientId(repository)(accountId, " account-client ")
        val updated = updatedAccount ?: error("Account was not updated")

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(updated.incomingServerSettings.extra).isEqualTo(
            mapOf(
                "unrelated" to "preserved",
                NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID to "account-client",
            ),
        )
        assertThat(updated.email).isEqualTo(existing.email)
        assertThat(updated.outgoingServerSettings).isEqualTo(existing.outgoingServerSettings)
    }

    @Test
    fun `does not update a non Gmail or Graph account`() = runTest {
        val accountId = AccountIdFactory.create()
        val existing = createAccount(
            accountId = accountId,
            serverSettings = gmailSettings().copy(type = Protocols.IMAP),
        )
        var updateCalled = false
        val repository = object : LegacyAccountRepository {
            override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = flowOf(existing)
            override suspend fun update(account: LegacyAccount) {
                updateCalled = true
            }
        }

        val result = UpdateApiClientId(repository)(accountId, "account-client")

        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat(updateCalled).isEqualTo(false)
    }

    @Test
    fun `returns NotFound when account does not exist`() = runTest {
        val repository = object : LegacyAccountRepository {
            override fun observeById(accountId: AccountId): Flow<LegacyAccount?> = emptyFlow()
            override suspend fun update(account: LegacyAccount) = Unit
        }

        val result = UpdateApiClientId(repository)(AccountIdFactory.create(), "account-client")

        assertThat(result).isInstanceOf(Outcome.Failure::class)
    }

    private fun createAccount(accountId: AccountId, serverSettings: ServerSettings) = LegacyAccount(
        id = accountId,
        name = "Account name",
        email = "user@example.com",
        profile = ProfileDto(
            id = accountId,
            name = "Account name",
            color = 0xFF0000,
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
                description = "Test identity",
            ),
        ),
        incomingServerSettings = serverSettings,
        outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user@example.com",
            password = null,
            clientCertificateAlias = null,
        ),
    )

    private fun gmailSettings(extra: Map<String, String?> = emptyMap()) = ServerSettings(
        type = Protocols.GMAIL_API,
        host = "gmail.googleapis.com",
        port = 443,
        connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
        authenticationType = AuthType.XOAUTH2,
        username = "user@example.com",
        password = null,
        clientCertificateAlias = null,
        extra = extra,
    )
}
