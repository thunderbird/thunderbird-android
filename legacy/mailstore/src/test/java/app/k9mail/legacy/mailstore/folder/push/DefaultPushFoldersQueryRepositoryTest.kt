package app.k9mail.legacy.mailstore.folder.push

import app.cash.turbine.test
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.MoreMessages
import app.k9mail.legacy.mailstore.RemoteFolderDetails
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val INBOX_FOLDER_ID = 1L
private const val ARCHIVE_FOLDER_ID = 2L

class DefaultPushFoldersQueryRepositoryTest {
    private val account = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val accountId = account.id
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakePushFoldersLegacyAccountDtoManager(accounts = listOf(account))
    private val messageStoreFactory = FakePushFoldersMessageStoreFactory(
        messageStoresByUuid = mapOf(account.uuid to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)
    private val testSubject = DefaultPushFoldersQueryRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `getAllByAccountId should return Success with push enabled folders`() {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
            FakeFolderDetailsAccessor(id = ARCHIVE_FOLDER_ID, name = "Archive", isPushEnabled = false),
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
                ),
            ),
        )
    }

    @Test
    fun `getAllByAccountId should return Failure with NotFound when there are no push enabled folders`() {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = false),
        )

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.NotFound))
    }

    @Test
    fun `getAllByAccountId should return Failure with NotFound when there are no folders at all`() {
        // Arrange
        stubGetFolders()

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.NotFound))
    }

    @Test
    fun `getAllByAccountId should throw when account does not exist`() {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act & Assert
        assertFailure {
            testSubject.getAllByAccountId(unknownAccountId)
        }.isInstanceOf<IllegalStateException>()
    }

    @Test
    fun `getAllByAccountId should rethrow MessagingException thrown by the message store`() {
        // Arrange
        val exception = MessagingException("failed to fetch folders")
        doThrow(exception).whenever(messageStore).getFolders<RemoteFolderDetails>(any(), any())

        // Act & Assert
        assertFailure {
            testSubject.getAllByAccountId(accountId)
        }.isEqualTo(exception)
    }

    @Test
    fun `observeAllByAccountId should emit the current push folders on subscription`() = runTest {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
        )

        // Act & Assert
        testSubject.observeAllByAccountId(accountId).test {
            assertThat(awaitItem()).isEqualTo(
                Outcome.success(
                    listOf(
                        RemoteFolder(
                            id = INBOX_FOLDER_ID,
                            serverId = "serverId",
                            name = "Inbox",
                            type = FolderType.REGULAR,
                        ),
                    ),
                ),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllByAccountId should emit an updated result when folder settings change`() = runTest {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = false),
        )
        val listenerCaptor = argumentCaptor<FolderSettingsChangedListener>()

        // Act & Assert
        testSubject.observeAllByAccountId(accountId).test {
            assertThat(awaitItem()).isEqualTo(Outcome.failure(FolderError.NotFound))

            verify(messageStore).addFolderSettingsChangedListener(listenerCaptor.capture())
            stubGetFolders(
                FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
            )
            listenerCaptor.firstValue.onFolderSettingsChanged()

            assertThat(awaitItem()).isEqualTo(
                Outcome.success(
                    listOf(
                        RemoteFolder(
                            id = INBOX_FOLDER_ID,
                            serverId = "serverId",
                            name = "Inbox",
                            type = FolderType.REGULAR,
                        ),
                    ),
                ),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllByAccountId should not emit duplicate consecutive results`() = runTest {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
        )
        val listenerCaptor = argumentCaptor<FolderSettingsChangedListener>()

        // Act & Assert
        testSubject.observeAllByAccountId(accountId).test {
            assertThat(awaitItem()).isInstanceOf(Outcome.Success::class)

            verify(messageStore).addFolderSettingsChangedListener(listenerCaptor.capture())
            listenerCaptor.firstValue.onFolderSettingsChanged()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllByAccountId should remove the folder settings listener when the flow is cancelled`() = runTest {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
        )
        val listenerCaptor = argumentCaptor<FolderSettingsChangedListener>()

        // Act
        testSubject.observeAllByAccountId(accountId).test {
            awaitItem()
            verify(messageStore).addFolderSettingsChangedListener(listenerCaptor.capture())
            cancelAndIgnoreRemainingEvents()
        }

        // Assert
        verify(messageStore).removeFolderSettingsChangedListener(listenerCaptor.firstValue)
    }

    @Test
    fun `observeAllByAccountId should throw when account does not exist`() = runTest {
        // Arrange
        val unknownAccountId = AccountIdFactory.of(ACCOUNT_ID_OTHER_RAW)

        // Act & Assert
        testSubject.observeAllByAccountId(unknownAccountId).test {
            assertThat(awaitError()).isInstanceOf(IllegalStateException::class)
        }
    }

    private fun stubGetFolders(vararg accessors: FolderDetailsAccessor) {
        whenever(messageStore.getFolders<RemoteFolderDetails>(eq(true), any())).thenAnswer { invocation ->
            val mapper = invocation.getArgument<FolderMapper<RemoteFolderDetails>>(1)
            accessors.map { mapper.map(it) }
        }
    }
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

private class FakePushFoldersLegacyAccountDtoManager(
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

private class FakePushFoldersMessageStoreFactory(
    private val messageStoresByUuid: Map<String, ListenableMessageStore>,
) : MessageStoreFactory {
    override fun create(account: LegacyAccountDto): ListenableMessageStore = messageStoresByUuid.getValue(account.uuid)
}

