package net.thunderbird.app.common.feature.mail

import assertk.assertThat
import assertk.assertions.isNotSameAs
import assertk.assertions.isSameAs
import kotlin.test.Test
import net.thunderbird.core.common.mail.Protocols

class RemoteFolderCreatorResolverTest {

    private val imapFactory = FakeImapRemoteFolderCreatorFactory()

    private val router = RemoteFolderCreatorResolver(imapFactory = imapFactory)

    @Test
    fun `when account is LegacyAccountDto and IMAP then delegate to imapFactory`() {
        // Arrange
        val account = FakeData.legacyAccountDto.apply {
            incomingServerSettings = incomingServerSettings.copy(type = Protocols.IMAP)
            outgoingServerSettings = outgoingServerSettings.copy(type = Protocols.IMAP)
        }

        // Act
        val result = router.create(account)

        // Assert
        assertThat(result).isNotSameAs(NoOpRemoteFolderCreator)
    }

    @Test
    fun `when account is LegacyAccountDto and POP3 then return NoOp`() {
        // Arrange
        val account = FakeData.legacyAccountDto.apply {
            incomingServerSettings = incomingServerSettings.copy(type = Protocols.POP3)
            outgoingServerSettings = outgoingServerSettings.copy(type = Protocols.POP3)
        }

        // Act
        val result = router.create(account)

        // Assert
        assertThat(result).isSameAs(NoOpRemoteFolderCreator)
    }
}
