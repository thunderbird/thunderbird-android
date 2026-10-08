package net.thunderbird.app.common.feature.mail

import assertk.assertThat
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.app.common.account.data.FakeLegacyAccountManager
import net.thunderbird.core.common.mail.Protocols

class RemoteFolderCreatorResolverTest {

    private val accountManager = FakeLegacyAccountManager()

    private val imapFactory = FakeImapRemoteFolderCreatorFactory()

    private val router = RemoteFolderCreatorResolver(
        accountManager = accountManager,
        imapFactory = imapFactory,
    )

    @Test
    fun `when account is IMAP then delegate to imapFactory`() = runTest {
        // Arrange
        val account = FakeData.legacyAccount.copy(
            incomingServerSettings = FakeData.legacyAccount.incomingServerSettings.copy(type = Protocols.IMAP),
        )
        accountManager.update(account)

        // Act
        val result = router.create(account.id)

        // Assert
        assertThat(result).isNotSameInstanceAs(NoOpRemoteFolderCreator)
    }

    @Test
    fun `when account is POP3 then return NoOp`() = runTest {
        // Arrange
        val account = FakeData.legacyAccount.copy(
            incomingServerSettings = FakeData.legacyAccount.incomingServerSettings.copy(type = Protocols.POP3),
        )
        accountManager.update(account)

        // Act
        val result = router.create(account.id)

        // Assert
        assertThat(result).isSameInstanceAs(NoOpRemoteFolderCreator)
    }
}
