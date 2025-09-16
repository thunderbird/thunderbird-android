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
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import org.junit.Test

class BaseAccountSpecialFolderUpdaterFactoryTest {

    @Test
    fun `delegates to legacy factory for LegacyAccountDto`() {
        // Arrange
        val dto = createLegacyAccountDto()
        val legacyFactory = FakeLegacyAccountDtoSpecialFolderUpdaterFactory()
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = dto }
        val factory = BaseAccountSpecialFolderUpdaterFactory(legacyFactory, mapper)

        // Act
        val updater = factory.create(dto)

        // Assert
        assertThat(legacyFactory.lastAccount).isSameAs(dto)
        assertThat(updater).isInstanceOf(SpecialFolderUpdater::class)
    }

    @Test
    fun `maps LegacyAccount to dto and delegates`() {
        // Arrange
        val dto = createLegacyAccountDto()
        val domain = createLegacyAccountFromDto(dto)
        val legacyFactory = FakeLegacyAccountDtoSpecialFolderUpdaterFactory()
        val mapper = FakeLegacyAccountDataMapper().apply { toDtoResult = dto }
        val factory = BaseAccountSpecialFolderUpdaterFactory(legacyFactory, mapper)

        // Act
        factory.create(domain)

        // Assert
        assertThat(mapper.lastMapped).isSameAs(domain)
        assertThat(legacyFactory.lastAccount).isSameAs(dto)
    }

    @Test
    fun `unsupported BaseAccount throws`() {
        // Arrange
        val unsupported: BaseAccount = FakeData.unsupportedAccount
        val legacyFactory = FakeLegacyAccountDtoSpecialFolderUpdaterFactory()
        val mapper = FakeLegacyAccountDataMapper()
        val factory = BaseAccountSpecialFolderUpdaterFactory(legacyFactory, mapper)

        // Act + Assert
        assertFailure { factory.create(unsupported) }
            .isInstanceOf(IllegalArgumentException::class)
    }

    private fun createLegacyAccountDto(): LegacyAccountDto {
        val dto = LegacyAccountDto(uuid = net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW)
        dto.identities = mutableListOf(net.thunderbird.core.android.account.Identity(email = "user@example.com"))
        dto.email = "user@example.com"
        dto.incomingServerSettings = ServerSettings(
            type = net.thunderbird.core.common.mail.Protocols.IMAP,
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        dto.outgoingServerSettings = ServerSettings(
            type = net.thunderbird.core.common.mail.Protocols.IMAP,
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "pass",
            clientCertificateAlias = null,
        )
        return dto
    }

    private fun createLegacyAccountFromDto(dto: LegacyAccountDto): LegacyAccount {
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
}
