package app.k9mail.legacy.mailstore.folder

import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.MoreMessages
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.AccountRemovedListener
import net.thunderbird.core.android.account.AccountsChangeListener
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

private const val INBOX_FOLDER_ID = 1L
private const val ARCHIVE_FOLDER_ID = 2L

@Suppress("MaxLineLength")
class DefaultRemoteFolderQueryRepositoryTest {
    private val account = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val accountId = account.id
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakeRemoteFolderLegacyAccountDtoManager(accounts = listOf(account))
    private val messageStoreFactory = FakeRemoteFolderMessageStoreFactory(
        messageStoresByUuid = mapOf(account.uuid to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)
    private val testSubject = DefaultRemoteFolderQueryRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `getAllByAccountId should return Success with all remote folders`() = runTest {
        // Arrange
        stubGetFolders(
            FakeRemoteFolderAccessor(id = INBOX_FOLDER_ID, name = "Inbox"),
            FakeRemoteFolderAccessor(id = ARCHIVE_FOLDER_ID, name = "Archive"),
        )

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(
            Outcome.success(
                listOf(
                    RemoteFolder(
                        id = INBOX_FOLDER_ID,
                        serverId = "serverId",
                        name = "Inbox",
                        type = FolderType.REGULAR,
                    ),
                    RemoteFolder(
                        id = ARCHIVE_FOLDER_ID,
                        serverId = "serverId",
                        name = "Archive",
                        type = FolderType.REGULAR,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `getAllByAccountId should return Success with an empty list when there are no folders`() = runTest {
        // Arrange
        stubGetFolders()

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.success(emptyList()))
    }

    @Test
    fun `getAllByAccountId should return Failure with AccountNotFound when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act
        val result = testSubject.getAllByAccountId(unknownAccountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.AccountNotFound))
    }

    @Test
    fun `getAllByAccountId should return Failure with FailedToQueryDatabase when the message store throws MessagingException`() =
        runTest {
            // Arrange
            val exception = MessagingException("failed to fetch folders")
            doThrow(exception).whenever(messageStore).getFolders<RemoteFolder>(any(), any())

            // Act
            val result = testSubject.getAllByAccountId(accountId)

            // Assert
            assertThat(result).isEqualTo(
                Outcome.failure(
                    FolderError.FailedToQueryDatabase(message = "Failed to query database.", throwable = exception),
                ),
            )
        }

    private fun stubGetFolders(vararg accessors: FolderDetailsAccessor) {
        whenever(messageStore.getFolders<RemoteFolder>(eq(true), any())).thenAnswer { invocation ->
            val mapper = invocation.getArgument<FolderMapper<RemoteFolder>>(1)
            accessors.map { mapper.map(it) }
        }
    }
}

private class FakeRemoteFolderAccessor(
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

private class FakeRemoteFolderLegacyAccountDtoManager(
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

private class FakeRemoteFolderMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore =
        messageStoresByUuid.getValue(account.uuid)
}
