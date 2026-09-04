package app.k9mail.legacy.mailstore.folder

import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.MoreMessages
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
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
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.PartialUpdatableFolderDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val OUTBOX_FOLDER_ID = 100L
private const val INBOX_FOLDER_ID = 1L
private const val REGULAR_FOLDER_ID = 42L

@Suppress("MaxLineLength")
class DefaultFolderDetailsRepositoryTest {
    private val accountId = AccountIdFactory.of(ACCOUNT_ID_RAW)
    private val account = createLegacyAccount(accountId)
    private val accountDto = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakeLegacyAccountManager(accounts = listOf(account))
    private val messageStoreManager = MessageStoreManager(
        accountManager = FakeLegacyAccountDtoManager(accounts = listOf(accountDto)),
        messageStoreFactory = FakeMessageStoreFactory(
            messageStoresByUuid = mapOf(accountDto.uuid to messageStore),
        ),
    )
    private val outboxFolderManager = FakeOutboxFolderManager(outboxFolderId = OUTBOX_FOLDER_ID)
    private val testSubject = DefaultFolderDetailsRepository(
        logger = TestLogger(),
        accountManager = accountManager,
        outboxFolderManager = outboxFolderManager,
        messageStoreManager = messageStoreManager,
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
    fun `findById should return Success with null when folder does not exist`() = runTest {
        // Arrange
        whenever(messageStore.getFolder<FolderDetails?>(eq(REGULAR_FOLDER_ID), any())).thenReturn(null)

        // Act
        val result = testSubject.findById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat((result as Outcome.Success).data).isNull()
    }

    @Test
    fun `findById should return Success with mapped folder details for a regular folder`() = runTest {
        // Arrange
        val accessor = FakeFolderDetailsAccessor(
            id = REGULAR_FOLDER_ID,
            name = "Regular",
            isInTopGroup = true,
            isIntegrate = true,
            isSyncEnabled = true,
            isVisible = true,
            isNotificationsEnabled = false,
            isPushEnabled = true,
        )
        stubGetFolder(REGULAR_FOLDER_ID, accessor)

        // Act
        val result = testSubject.findById(accountId, REGULAR_FOLDER_ID)

        // Assert
        assertThat(result).isInstanceOf(Outcome.Success::class)
        val folderDetails = (result as Outcome.Success).data
        assertThat(folderDetails?.folder?.id).isEqualTo(REGULAR_FOLDER_ID)
        assertThat(folderDetails?.folder?.name).isEqualTo("Regular")
        assertThat(folderDetails?.folder?.type).isEqualTo(FolderType.REGULAR)
        assertThat(folderDetails?.isInTopGroup).isEqualTo(true)
        assertThat(folderDetails?.isIntegrate).isEqualTo(true)
        assertThat(folderDetails?.isSyncEnabled).isEqualTo(true)
        assertThat(folderDetails?.isVisible).isEqualTo(true)
        assertThat(folderDetails?.isNotificationsEnabled).isEqualTo(false)
        assertThat(folderDetails?.isPushEnabled).isEqualTo(true)
    }

    @Test
    fun `findById should map folder type to INBOX when folder id matches account inbox folder id`() = runTest {
        // Arrange
        stubGetFolder(INBOX_FOLDER_ID, FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID))

        // Act
        val result = testSubject.findById(accountId, INBOX_FOLDER_ID)

        // Assert
        val folderDetails = (result as Outcome.Success).data
        assertThat(folderDetails?.folder?.type).isEqualTo(FolderType.INBOX)
    }

    @Test
    fun `findById should map folder type to OUTBOX when folder id matches the outbox folder id`() = runTest {
        // Arrange
        stubGetFolder(OUTBOX_FOLDER_ID, FakeFolderDetailsAccessor(id = OUTBOX_FOLDER_ID))

        // Act
        val result = testSubject.findById(accountId, OUTBOX_FOLDER_ID)

        // Assert
        val folderDetails = (result as Outcome.Success).data
        assertThat(folderDetails?.folder?.type).isEqualTo(FolderType.OUTBOX)
    }

    @Test
    fun `update should update folder settings and return Success`() = runTest {
        // Arrange
        val folderDetails = createFolderDetails(id = REGULAR_FOLDER_ID)

        // Act
        val result = testSubject.update(accountId, folderDetails)

        // Assert
        verify(messageStore).updateFolderSettings(folderDetails)
        assertThat(result).isEqualTo(Outcome.success(Unit))
    }

    @Test
    fun `update should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)
        val folderDetails = createFolderDetails(id = REGULAR_FOLDER_ID)

        // Act
        val result = testSubject.update(unknownAccountId, folderDetails)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `update should return Failure with FailedPrecondition when message store throws IllegalArgumentException`() =
        runTest {
            // Arrange
            val folderDetails = createFolderDetails(id = REGULAR_FOLDER_ID)
            doThrow(IllegalArgumentException("missing fields")).whenever(messageStore).updateFolderSettings(any())

            // Act
            val result = testSubject.update(accountId, folderDetails)

            // Assert
            assertThat(result).isInstanceOf(Outcome.Failure::class)
            assertThat((result as Outcome.Failure).error).isInstanceOf(FolderError.FailedPrecondition::class)
        }

    @Test
    fun `partial update should only apply the provided fields`() = runTest {
        // Arrange
        val partialUpdate = PartialUpdatableFolderDetails(folderId = REGULAR_FOLDER_ID, syncEnabled = true)

        // Act
        val result = testSubject.update(accountId, partialUpdate)

        // Assert
        verify(messageStore).setSyncEnabled(REGULAR_FOLDER_ID, true)
        verify(messageStore, never()).setIncludeInUnifiedInbox(any(), any())
        verify(messageStore, never()).setVisible(any(), any())
        verify(messageStore, never()).setNotificationsEnabled(any(), any())
        verify(messageStore, never()).setPushEnabled(any(), any())
        assertThat(result).isEqualTo(Outcome.success(Unit))
    }

    @Test
    fun `partial update should update unified inbox inclusion when provided`() = runTest {
        // Arrange
        val partialUpdate = PartialUpdatableFolderDetails(
            folderId = REGULAR_FOLDER_ID,
            includeInUnifiedInbox = true,
        )
        // Act
        val result = testSubject.update(accountId, partialUpdate)
        // Assert
        verify(messageStore).setIncludeInUnifiedInbox(REGULAR_FOLDER_ID, true)
        assertThat(result).isEqualTo(Outcome.success(Unit))
    }

    @Test
    fun `partial update should apply all fields when all are provided`() = runTest {
        // Arrange
        val partialUpdate = PartialUpdatableFolderDetails(
            folderId = REGULAR_FOLDER_ID,
            includeInUnifiedInbox = true,
            syncEnabled = false,
            visible = true,
            notificationsEnabled = false,
            isPushEnabled = true,
        )

        // Act
        val result = testSubject.update(accountId, partialUpdate)

        // Assert
        verify(messageStore).setIncludeInUnifiedInbox(REGULAR_FOLDER_ID, true)
        verify(messageStore).setSyncEnabled(REGULAR_FOLDER_ID, false)
        verify(messageStore).setVisible(REGULAR_FOLDER_ID, true)
        verify(messageStore).setNotificationsEnabled(REGULAR_FOLDER_ID, false)
        verify(messageStore).setPushEnabled(REGULAR_FOLDER_ID, true)
        assertThat(result).isEqualTo(Outcome.success(Unit))
    }

    @Test
    fun `partial update should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)
        val partialUpdate = PartialUpdatableFolderDetails(folderId = REGULAR_FOLDER_ID, syncEnabled = true)

        // Act
        val result = testSubject.update(unknownAccountId, partialUpdate)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `partial update should return Failure with FailedPrecondition when message store throws IllegalArgumentException`() =
        runTest {
            // Arrange
            val partialUpdate = PartialUpdatableFolderDetails(folderId = REGULAR_FOLDER_ID, syncEnabled = true)
            doThrow(IllegalArgumentException("invalid state"))
                .whenever(messageStore).setSyncEnabled(any(), any())

            // Act
            val result = testSubject.update(accountId, partialUpdate)

            // Assert
            assertThat(result).isInstanceOf(Outcome.Failure::class)
            assertThat((result as Outcome.Failure).error).isInstanceOf(FolderError.FailedPrecondition::class)
        }

    private fun stubGetFolder(folderId: Long, accessor: FolderDetailsAccessor) {
        whenever(messageStore.getFolder<FolderDetails?>(eq(folderId), any())).thenAnswer { invocation ->
            val mapper = invocation.getArgument<FolderMapper<FolderDetails?>>(1)
            mapper.map(accessor)
        }
    }
}

private fun createFolderDetails(id: Long): FolderDetails {
    return FolderDetails(
        folder = Folder(
            id = id,
            name = "Folder",
            type = FolderType.REGULAR,
            isLocalOnly = false,
        ),
        isInTopGroup = false,
        isIntegrate = false,
        isSyncEnabled = true,
        isVisible = true,
        isNotificationsEnabled = true,
        isPushEnabled = false,
    )
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

private class FakeLegacyAccountManager(
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

private class FakeLegacyAccountDtoManager(
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

private class FakeMessageStoreFactory(
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
