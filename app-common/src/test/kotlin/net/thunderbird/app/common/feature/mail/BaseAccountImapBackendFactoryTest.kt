package net.thunderbird.app.common.feature.mail

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import org.junit.Test

class BaseAccountImapBackendFactoryTest {

    @Test
    fun `imap dto delegates to legacy factory`() {
        // Arrange
        val account = createLegacyAccountDto(protocol = Protocols.IMAP)
        val legacyFactory = FakeLegacyBackendFactory()
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = account }
        val factory = BaseAccountImapBackendFactory(legacyFactory, mapper)

        // Act
        factory.createBackend(account)

        // Assert
        assertThat(legacyFactory.lastAccount).isSameAs(account)
    }

    @Test
    fun `imap domain account is mapped to dto then delegated`() {
        // Arrange
        val dto = createLegacyAccountDto(protocol = Protocols.IMAP)
        val domain = createLegacyAccountFromDto(dto)
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = dto }
        val legacyFactory = FakeLegacyBackendFactory()
        val factory = BaseAccountImapBackendFactory(legacyFactory, mapper)

        // Act
        factory.createBackend(domain)

        // Assert
        assertThat(mapper.lastMapped).isSameAs(domain)
        assertThat(legacyFactory.lastAccount).isSameAs(dto)
    }

    @Test
    fun `pop3 account throws IllegalArgumentException`() {
        // Arrange
        val account = createLegacyAccountDto(protocol = Protocols.POP3)
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = account }
        val legacyFactory = FakeLegacyBackendFactory()
        val factory = BaseAccountImapBackendFactory(legacyFactory, mapper)

        // Act + Assert
        assertFailure { factory.createBackend(account) }
            .isInstanceOf(IllegalArgumentException::class)
    }

    private fun createLegacyAccountFromDto(dto: LegacyAccountDto): LegacyAccount {
        // Arrange helper: Build a minimal domain account based on dto values
        return LegacyAccount(
            id = dto.id,
            name = dto.name,
            profile = net.thunderbird.feature.account.storage.profile.ProfileDto(
                id = dto.id,
                name = dto.displayName,
                color = dto.chipColor,
                avatar = dto.avatar,
            ),
            identities = dto.identities,
            email = dto.email,
            deletePolicy = dto.deletePolicy,
            incomingServerSettings = dto.incomingServerSettings,
            outgoingServerSettings = dto.outgoingServerSettings,
        )
    }

    private fun createLegacyAccountDto(protocol: String): LegacyAccountDto {
        // Arrange helper
        val account = LegacyAccountDto(uuid = net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW)
        // Initialize identities to allow email setter usage
        account.identities = mutableListOf(net.thunderbird.core.android.account.Identity(email = "user@example.com"))
        // Ensure email property is set via identity[0]
        account.email = "user@example.com"
        account.incomingServerSettings = ServerSettings(
            type = protocol,
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        account.outgoingServerSettings = ServerSettings(
            type = protocol,
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        return account
    }
}
