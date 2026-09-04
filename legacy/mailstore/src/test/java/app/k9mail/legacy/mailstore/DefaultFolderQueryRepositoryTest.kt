package app.k9mail.legacy.mailstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.data.FolderError
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
    private val accountDto = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakeFolderQueryLegacyAccountManager(accounts = listOf(account))
    private val outboxFolderManager = FakeOutboxFolderManager(outboxFolderId = OUTBOX_FOLDER_ID)
    private var messageStoreManager = createMessageStoreManager(accountDto)
    private val testSubject = DefaultFolderQueryRepository(
        logger = TestLogger(),
        accountManager = accountManager,
        messageStoreManager = messageStoreManager,
        outboxFolderManager = outboxFolderManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `findById should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act
        val result = testSubject.findById(unknownAccountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `findById should return Failure with AccountNotFound when the message store is not found`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountDto = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.findById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
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
    fun `findFolderServerIdById should return Failure with AccountNotFound when the message store is not found`() =
        runTest {
            // Arrange
            val subject = DefaultFolderQueryRepository(
                logger = TestLogger(),
                accountManager = accountManager,
                messageStoreManager = createMessageStoreManager(accountDto = null),
                outboxFolderManager = outboxFolderManager,
                ioDispatcher = Dispatchers.Unconfined,
            )

            // Act
            val result = subject.findFolderServerIdById(accountId, REGULAR_FOLDER_ID)

            // Assert
            assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
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
    fun `findIdByServerId should return Failure with AccountNotFound when the message store is not found`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountDto = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.findIdByServerId(accountId, FolderServerId("serverId"))

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
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
    fun `isPresent should return false when account does not exist`() = runTest {
        // Arrange
        val subject = DefaultFolderQueryRepository(
            logger = TestLogger(),
            accountManager = accountManager,
            messageStoreManager = createMessageStoreManager(accountDto = null),
            outboxFolderManager = outboxFolderManager,
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Act
        val result = subject.isPresent(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isFalse()
    }

    private fun createMessageStoreManager(accountDto: LegacyAccountDto?): MessageStoreManager {
        val accounts = accountDto?.let { listOf(it) } ?: emptyList()
        return MessageStoreManager(
            accountManager = FakeFolderQueryLegacyAccountDtoManager(accounts = accounts),
            messageStoreFactory = FakeFolderQueryMessageStoreFactory(
                messageStoresByUuid = accountDto?.let { mapOf(it.uuid to messageStore) } ?: emptyMap(),
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
            avatar = AvatarDto(AvatarTypeDto.MONOGRAM, "A", null, null),
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

private class FakeFolderDetailsAccessor(
    override val id: Long,
    override val name: String = "Folder",
    override val serverId: String? = "serverId",
    override val type: com.fsck.k9.mail.FolderType = com.fsck.k9.mail.FolderType.REGULAR,
    override val isLocalOnly: Boolean = false,
    override val isInTopGroup: Boolean = false,
    override val isIntegrate: Boolean = false,
    override val isSyncEnabled: Boolean = true,
    override val isVisible: Boolean = true,
    override val isNotificationsEnabled: Boolean = true,
    override val isPushEnabled: Boolean = false,
    override val visibleLimit: Int = 25,
    override val moreMessages: MoreMessages = MoreMessages.UNKNOWN,
    override val lastChecked: Long? = null,
    override val unreadMessageCount: Int = 0,
    override val starredMessageCount: Int = 0,
) : FolderDetailsAccessor {
    override fun serverIdOrThrow(): String = serverId ?: error("serverId is null")
}

private class FakeFolderQueryLegacyAccountManager(
    private val accounts: List<LegacyAccount> = emptyList(),
) : LegacyAccountManager {
    override fun getAll(): Flow<List<LegacyAccount>> = flowOf(accounts)
    override fun getById(id: AccountId): Flow<LegacyAccount?> = flowOf(accounts.find { it.id == id })
    override suspend fun update(account: LegacyAccount) = error("Not implemented")
    override fun getByIdSync(id: AccountId): LegacyAccount? = accounts.find { it.id == id }
    override fun updateSync(account: LegacyAccount) = error("Not implemented")
    override fun getAccounts(): List<LegacyAccount> = accounts
    override fun getAccountsFlow(): Flow<List<LegacyAccount>> = flowOf(accounts)
    override fun getAccount(accountUuid: String): LegacyAccount? = accounts.find { it.uuid == accountUuid }
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccount?> = flowOf(getAccount(accountUuid))
    override fun moveAccount(account: LegacyAccount, newPosition: Int) = error("Not implemented")
    override fun saveAccount(account: LegacyAccount) = error("Not implemented")
}

private class FakeFolderQueryLegacyAccountDtoManager(
    accounts: List<LegacyAccountDto> = emptyList(),
) : LegacyAccountDtoManager {
    private val accountsByUuid = accounts.associateBy { it.uuid }

    override fun getAccounts(): List<LegacyAccountDto> = accountsByUuid.values.toList()
    override fun getAccountsFlow(): Flow<List<LegacyAccountDto>> = flowOf(getAccounts())
    override fun getAccount(accountUuid: String): LegacyAccountDto? = accountsByUuid[accountUuid]
    override fun getAccountFlow(accountUuid: String): Flow<LegacyAccountDto?> = flowOf(getAccount(accountUuid))
    override fun addAccountRemovedListener(listener: AccountRemovedListener) = Unit
    override fun moveAccount(account: LegacyAccountDto, newPosition: Int) = Unit
    override fun addOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun removeOnAccountsChangeListener(accountsChangeListener: AccountsChangeListener) = Unit
    override fun saveAccount(account: LegacyAccountDto) = Unit
}

private class FakeFolderQueryMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore = messageStoresByUuid.getValue(account.uuid)
}

private class FakeOutboxFolderManager(
    private val outboxFolderId: Long,
) : OutboxFolderManager {
    override suspend fun getOutboxFolderId(accountId: AccountId, createIfMissing: Boolean): Long = outboxFolderId
    override suspend fun createOutboxFolder(accountId: AccountId): Outcome<Long, Exception> =
        error("Not implemented")

    override suspend fun hasPendingMessages(accountId: AccountId): Boolean = error("Not implemented")
}
