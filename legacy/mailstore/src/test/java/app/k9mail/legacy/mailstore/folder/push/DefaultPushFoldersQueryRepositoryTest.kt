package app.k9mail.legacy.mailstore.folder.push

import app.cash.turbine.test
import app.k9mail.legacy.mailstore.FolderSettingsChangedListener
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import app.k9mail.legacy.mailstore.MessageStoreManager
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
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderDetailsRepository
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

private const val INBOX_FOLDER_ID = 1L
private const val ARCHIVE_FOLDER_ID = 2L

@Suppress("MaxLineLength")
class DefaultPushFoldersQueryRepositoryTest {
    private val account = LegacyAccountDto(ACCOUNT_ID_RAW)
    private val accountId = account.id
    private val messageStore = mock<ListenableMessageStore>()
    private val accountManager = FakePushFoldersLegacyAccountDtoManager(accounts = listOf(account))
    private val messageStoreFactory = FakePushFoldersMessageStoreFactory(
        messageStoresByUuid = mapOf(account.uuid to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(accountManager, messageStoreFactory)
    private val remoteFolderDetailsRepository = FakeRemoteFolderDetailsRepository()
    private val testSubject = DefaultPushFoldersQueryRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        remoteFolderDetailsRepository = remoteFolderDetailsRepository,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `getAllByAccountId should return Success with push enabled folders`() = runTest {
        // Arrange
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(
                createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true),
                createRemoteFolderDetails(id = ARCHIVE_FOLDER_ID, name = "Archive", isPushEnabled = false),
            ),
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
    fun `getAllByAccountId should return Failure with NotFound when there are no push enabled folders`() = runTest {
        // Arrange
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = false)),
        )

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.NotFound))
    }

    @Test
    fun `getAllByAccountId should return Failure with NotFound when there are no folders at all`() = runTest {
        // Arrange
        remoteFolderDetailsRepository.outcome = Outcome.success(emptyList())

        // Act
        val result = testSubject.getAllByAccountId(accountId)

        // Assert
        assertThat(result).isEqualTo(Outcome.failure(FolderError.NotFound))
    }

    @Test
    fun `getAllByAccountId should return Failure with NotFound when the folder details repository returns AccountNotFound`() =
        runTest {
            // Arrange
            remoteFolderDetailsRepository.outcome = Outcome.failure(FolderError.AccountNotFound)

            // Act
            val result = testSubject.getAllByAccountId(accountId)

            // Assert
            assertThat(result).isEqualTo(Outcome.failure(FolderError.NotFound))
        }

    @Test
    fun `getAllByAccountId should rethrow the throwable when the folder details repository returns FailedToQueryDatabase`() =
        runTest {
            // Arrange
            val exception = MessagingException("failed to fetch folders")
            remoteFolderDetailsRepository.outcome = Outcome.failure(
                FolderError.FailedToQueryDatabase(message = "Failed to query database.", throwable = exception),
            )

            // Act & Assert
            assertFailure {
                testSubject.getAllByAccountId(accountId)
            }.isEqualTo(exception)
        }

    @Test
    fun `observeAllByAccountId should emit the current push folders on subscription`() = runTest {
        // Arrange
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true)),
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
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = false)),
        )
        val listenerCaptor = argumentCaptor<FolderSettingsChangedListener>()

        // Act & Assert
        testSubject.observeAllByAccountId(accountId).test {
            assertThat(awaitItem()).isEqualTo(Outcome.failure(FolderError.NotFound))

            verify(messageStore).addFolderSettingsChangedListener(listenerCaptor.capture())
            remoteFolderDetailsRepository.outcome = Outcome.success(
                listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true)),
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
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true)),
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
        remoteFolderDetailsRepository.outcome = Outcome.success(
            listOf(createRemoteFolderDetails(id = INBOX_FOLDER_ID, name = "Inbox", isPushEnabled = true)),
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

    private fun createRemoteFolderDetails(
        id: Long,
        name: String,
        isPushEnabled: Boolean,
    ): RemoteFolderDetails = RemoteFolderDetails(
        folder = RemoteFolder(id = id, serverId = "serverId", name = name, type = FolderType.REGULAR),
        isInTopGroup = false,
        isIntegrate = false,
        isSyncEnabled = true,
        isVisible = true,
        isNotificationsEnabled = true,
        isPushEnabled = isPushEnabled,
    )
}

private class FakeRemoteFolderDetailsRepository(
    var outcome: Outcome<List<RemoteFolderDetails>, FolderError> = Outcome.success(emptyList()),
) : RemoteFolderDetailsRepository {
    override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolderDetails>, FolderError> =
        outcome
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
    override fun create(account: LegacyAccountDto): ListenableMessageStore =
        messageStoresByUuid.getValue(account.uuid)
}
