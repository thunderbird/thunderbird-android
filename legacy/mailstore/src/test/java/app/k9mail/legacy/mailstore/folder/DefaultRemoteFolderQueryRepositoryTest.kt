package app.k9mail.legacy.mailstore.folder

import app.k9mail.legacy.mailstore.FakeFolderDetailsAccessor
import app.k9mail.legacy.mailstore.FakeMessageStoreFactory
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.folder.FolderType
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
    private val accountId = ACCOUNT_ID
    private val messageStore = mock<ListenableMessageStore>()
    private val messageStoreFactory = FakeMessageStoreFactory(
        messageStoresById = mapOf(accountId to messageStore),
    )
    private val messageStoreManager = MessageStoreManager(messageStoreFactory)
    private val testSubject = DefaultRemoteFolderQueryRepository(
        logger = TestLogger(),
        messageStoreManager = messageStoreManager,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `getAllByAccountId should return Success with all remote folders`() = runTest {
        // Arrange
        stubGetFolders(
            FakeFolderDetailsAccessor(id = INBOX_FOLDER_ID, name = "Inbox"),
            FakeFolderDetailsAccessor(id = ARCHIVE_FOLDER_ID, name = "Archive"),
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
        assertThat(result).isInstanceOf(Outcome.Failure::class)
        assertThat((result as Outcome.Failure).error).isInstanceOf(FolderError.AccountNotFound::class)
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

