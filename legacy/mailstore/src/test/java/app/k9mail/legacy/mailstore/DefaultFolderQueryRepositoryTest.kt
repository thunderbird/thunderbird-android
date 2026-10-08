package app.k9mail.legacy.mailstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlin.test.assertFails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private const val OUTBOX_FOLDER_ID = 100L
private const val INBOX_FOLDER_ID = 1L
private const val REGULAR_FOLDER_ID = 42L

@Suppress("MaxLineLength")
class DefaultFolderQueryRepositoryTest {
    private val accountId = AccountIdFactory.of(ACCOUNT_ID_RAW)
    private val account = createLegacyAccount(accountId)
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakeLegacyAccountManager(accounts = listOf(account))
    private val outboxFolderManager = FakeOutboxFolderManager(outboxFolderId = OUTBOX_FOLDER_ID)
    private var messageStoreManager = createMessageStoreManager(accountId)
    private val testSubject = DefaultFolderQueryRepository(
        logger = TestLogger(),
        accountManager = accountManager,
        messageStoreManager = messageStoreManager,
        outboxFolderManager = outboxFolderManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `findById should throw IllegalStateException when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act
        val exception = assertFails { testSubject.findById(unknownAccountId, REGULAR_FOLDER_ID) }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Account not found: $unknownAccountId")
    }

    @Test
    fun `findById should throw IllegalStateException when the message store is not found`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountId = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val exception = assertFails { subject.findById(accountId, REGULAR_FOLDER_ID) }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Account not found: $accountId")
    }

    @Test
    fun `findById should return Success with mapped folder for a regular folder`() = runTest {
        // Arrange
        stubGetFolder<Folder?>(REGULAR_FOLDER_ID, FakeFolderDetailsAccessor(id = REGULAR_FOLDER_ID, name = "Regular"))

        // Act
        val result = testSubject.findById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(
            Outcome.success(
                Folder(
                    id = REGULAR_FOLDER_ID,
                    name = "Regular",
                    type = FolderType.REGULAR,
                    isLocalOnly = false,
                ),
            ),
        )
    }

    @Test
    fun `findById should map folder type to INBOX when folder id matches account inbox folder id`() = runTest {
        // Arrange
        stubGetFolder<Folder?>(INBOX_FOLDER_ID, FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox"))

        // Act
        val result = testSubject.findById(accountId, INBOX_FOLDER_ID)

        // Assert
        val folder = (result as Outcome.Success).data
        assertThat(folder?.type).isEqualTo(FolderType.INBOX)
    }

    @Test
    fun `findById should map folder type to OUTBOX when folder id matches the outbox folder id`() = runTest {
        // Arrange
        stubGetFolder<Folder?>(OUTBOX_FOLDER_ID, FakeFolderDetailsAccessor(id = OUTBOX_FOLDER_ID, name = "Outbox"))

        // Act
        val result = testSubject.findById(accountId, OUTBOX_FOLDER_ID)

        // Assert
        val folder = (result as Outcome.Success).data
        assertThat(folder?.type).isEqualTo(FolderType.OUTBOX)
    }

    @Test
    fun `findById should return Success with null when folder does not exist`() = runTest {
        // Arrange
        stubGetFolder<Folder?>(REGULAR_FOLDER_ID, accessor = null)

        // Act
        val result = testSubject.findById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(null))
    }

    @Test
    fun `findFolderServerIdById should throw IllegalStateException when the message store is not found`() =
        runTest {
            // Arrange
            val subject = DefaultFolderQueryRepository(
                logger = TestLogger(),
                accountManager = accountManager,
                messageStoreManager = createMessageStoreManager(accountId = null),
                outboxFolderManager = outboxFolderManager,
                ioDispatcher = Dispatchers.Unconfined,
            )

            // Act
            val exception = assertFails { subject.findFolderServerIdById(accountId, REGULAR_FOLDER_ID) }

            // Assert
            assertThat(exception)
                .isInstanceOf<IllegalStateException>()
                .prop(IllegalStateException::message)
                .isEqualTo("Account not found: $accountId")
        }

    @Test
    fun `findFolderServerIdById should return Success with the folder server id`() = runTest {
        // Arrange
        stubGetFolder<FolderServerId?>(
            REGULAR_FOLDER_ID,
            FakeFolderDetailsAccessor(id = REGULAR_FOLDER_ID, serverId = "serverId"),
        )

        // Act
        val result = testSubject.findFolderServerIdById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(FolderServerId("serverId")))
    }

    @Test
    fun `findFolderServerIdById should return Success with null when the folder has no server id`() = runTest {
        // Arrange
        stubGetFolder<FolderServerId?>(
            REGULAR_FOLDER_ID,
            FakeFolderDetailsAccessor(id = REGULAR_FOLDER_ID, serverId = null),
        )

        // Act
        val result = testSubject.findFolderServerIdById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(null))
    }

    @Test
    fun `findIdByServerId should throw IllegalStateException when the message store is not found`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountId = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val exception = assertFails { subject.findIdByServerId(accountId, FolderServerId("serverId")) }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Account not found: $accountId")
    }

    @Test
    fun `findIdByServerId should return Success with the folder id`() = runTest {
        // Arrange
        whenever(messageStore.getFolderId("serverId")).thenReturn(REGULAR_FOLDER_ID)

        // Act
        val result = testSubject.findIdByServerId(accountId, FolderServerId("serverId"))

        // Assert
        assertThat(result).isEqualTo(Outcome.success(REGULAR_FOLDER_ID))
    }

    @Test
    fun `findIdByServerId should return Success with null when the server id is not found`() = runTest {
        // Arrange
        whenever(messageStore.getFolderId("unknown")).thenReturn(null)

        // Act
        val result = testSubject.findIdByServerId(accountId, FolderServerId("unknown"))

        // Assert
        assertThat(result).isEqualTo(Outcome.success(null))
    }

    @Test
    fun `isPresent should return true when the folder is present`() = runTest {
        // Arrange
        whenever(messageStore.getFolder<Boolean>(eq(REGULAR_FOLDER_ID), any())).thenReturn(true)

        // Act
        val result = testSubject.isPresent(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isPresent should return false when the folder is not present`() = runTest {
        // Arrange
        whenever(messageStore.getFolder<Boolean>(eq(REGULAR_FOLDER_ID), any())).thenReturn(null)

        // Act
        val result = testSubject.isPresent(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `isPresent should should throw IllegalStateException when the message store is not found`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountId = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val exception = assertFails { subject.isPresent(accountId, REGULAR_FOLDER_ID) }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .prop(IllegalStateException::message)
            .isEqualTo("Account not found: $accountId")
    }

    private fun createMessageStoreManager(accountId: AccountId?): MessageStoreManager {
        return MessageStoreManager(
            messageStoreFactory = FakeMessageStoreFactory(
                messageStoresById = accountId?.let { mapOf(it to messageStore) } ?: emptyMap(),
            ),
        )
    }

    private fun <T> stubGetFolder(folderId: Long, accessor: FolderDetailsAccessor?) {
        whenever(messageStore.getFolder<T>(eq(folderId), any())).thenAnswer { invocation ->
            val mapper = invocation.getArgument<FolderMapper<T>>(1)
            accessor?.let { mapper.map(it) }
        }
    }
}

private fun createLegacyAccount(id: AccountId): LegacyAccount {
    return LegacyAccount(
        id = id,
        name = "Account",
        email = "user@example.com",
        profile = ProfileDto(
            id = id,
            name = "Account",
            color = 0,
            avatar = AvatarDto(id = id, AvatarTypeDto.MONOGRAM, "A", null, null),
        ),
        incomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        ),
        outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        ),
        identities = listOf(Identity(name = "Account", email = "user@example.com")),
        inboxFolderId = INBOX_FOLDER_ID,
    )
}

private class FakeOutboxFolderManager(
    private val outboxFolderId: Long,
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(accountId: AccountId, createIfMissing: Boolean): Long = outboxFolderId
    override suspend fun createOutboxFolder(accountId: AccountId): Outcome<Long, Exception> =
        error("Not implemented")

    override suspend fun hasPendingMessages(accountId: AccountId): Boolean = error("Not implemented")
}
