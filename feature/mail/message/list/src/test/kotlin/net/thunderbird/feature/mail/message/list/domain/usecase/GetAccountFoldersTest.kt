package net.thunderbird.feature.mail.message.list.domain.usecase

import app.k9mail.legacy.mailstore.FolderRepository
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.domain.AccountFolderError
import org.mockito.Mockito.`when`
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

private const val VALID_ACCOUNT_UUID = "valid_account_uuid"
private const val INVALID_ACCOUNT_UUID = "invalid_account_uuid"

@Suppress("MaxLineLength")
class GetAccountFoldersTest {

    @Test
    fun `invoke should return REGULAR and ARCHIVE folders when repository returns a list of folders`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val regularFoldersSize = 10
        val remoteFolders = createRemoteFolders(
            regularFoldersSize = regularFoldersSize,
            addInboxFolder = true,
            addOutboxFolder = true,
            addSentFolder = true,
            addTrashFolder = true,
            addArchiveFolder = true,
            addSpamFolder = true,
        )
        val testSubject = createTestSubject(accountUuid, remoteFolders)

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Success<List<RemoteFolder>>>()
            .prop(name = "data") { it.data }
            .all {
                hasSize(regularFoldersSize + 1) // +1 counting Archive folder.
                transform { remoteFolders -> remoteFolders.map { it.type } }
                    .containsOnly(FolderType.REGULAR, FolderType.ARCHIVE)
            }
    }

    @Test
    fun `invoke should return only REGULAR folders when repository returns only REGULAR folders`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val regularFoldersSize = Random.nextInt(from = 1, until = 100)
        val remoteFolders = createRemoteFolders(
            regularFoldersSize = regularFoldersSize,
        )
        val testSubject = createTestSubject(accountUuid, remoteFolders)

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Success<List<RemoteFolder>>>()
            .prop(name = "data") { it.data }
            .all {
                hasSize(regularFoldersSize)
                transform { remoteFolders -> remoteFolders.map { it.type } }
                    .containsOnly(FolderType.REGULAR)
            }
    }

    @Test
    fun `invoke should return only ARCHIVE folder when repository returns only ARCHIVE folder`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val remoteFolders = createRemoteFolders(
            regularFoldersSize = 0,
            addArchiveFolder = true,
        )
        val testSubject = createTestSubject(accountUuid, remoteFolders)

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Success<List<RemoteFolder>>>()
            .prop(name = "data") { it.data }
            .all {
                hasSize(1)
                transform { remoteFolders -> remoteFolders.map { it.type } }
                    .containsOnly(FolderType.ARCHIVE)
            }
    }

    @Test
    fun `invoke should return an empty list when repository returns no REGULAR or ARCHIVE folders`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val remoteFolders = createRemoteFolders(
            regularFoldersSize = 0,
            addInboxFolder = true,
            addOutboxFolder = true,
            addSentFolder = true,
            addTrashFolder = true,
            addArchiveFolder = false,
            addSpamFolder = true,
        )
        val testSubject = createTestSubject(accountUuid, remoteFolders)

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Success<List<RemoteFolder>>>()
            .prop(name = "data") { it.data }
            .isEmpty()
    }

    @Test
    fun `invoke should return failure when repository throws MessagingException`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val errorMessage = "this is an error"
        val messagingException = MessagingException(errorMessage)
        val remoteFolders = listOf<RemoteFolder>()
        val testSubject = createTestSubject(
            accountUuid = accountUuid,
            folders = remoteFolders,
            exception = messagingException,
        )

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Failure<AccountFolderError>>()
            .prop("error") { it.error }
            .prop(AccountFolderError::exception)
            .isInstanceOf<MessagingException>()
            .hasMessage(errorMessage)
    }

    @Test
    fun `invoke should propagate exception when repository throws other types of exceptions`() = runTest {
        // Arrange
        val accountUuid = VALID_ACCOUNT_UUID
        val errorMessage = "not handled exception"
        val messagingException = RuntimeException(errorMessage)
        val remoteFolders = listOf<RemoteFolder>()
        val testSubject = createTestSubject(
            accountUuid = accountUuid,
            folders = remoteFolders,
            exception = messagingException,
        )

        // Act & Assert
        assertFailure { testSubject(accountUuid) }
            .isInstanceOf<RuntimeException>()
            .hasMessage(errorMessage)
    }

    @Test
    fun `invoke should handle invalid or non-existent account UUID`() = runTest {
        // Arrange
        val accountUuid = INVALID_ACCOUNT_UUID
        val remoteFolders = createRemoteFolders(
            regularFoldersSize = 100,
            addInboxFolder = true,
            addOutboxFolder = true,
            addSentFolder = true,
            addTrashFolder = true,
            addArchiveFolder = true,
            addSpamFolder = true,
        )
        val testSubject = createTestSubject(accountUuid, remoteFolders)

        // Act
        val folders = testSubject(accountUuid)

        // Assert
        assertThat(folders)
            .isInstanceOf<Outcome.Success<List<RemoteFolder>>>()
            .prop(name = "data") { it.data }
            .isEmpty()
    }

    private fun createRemoteFolders(
        regularFoldersSize: Int,
        addInboxFolder: Boolean = false,
        addOutboxFolder: Boolean = false,
        addSentFolder: Boolean = false,
        addTrashFolder: Boolean = false,
        addArchiveFolder: Boolean = false,
        addSpamFolder: Boolean = false,
    ): List<RemoteFolder> {
        fun createRemoteFolder(id: Long, type: FolderType) = RemoteFolder(
            id = id,
            name = "${type.name}-$id",
            serverId = "${type.name}-$id",
            type = type,
        )
        return buildList {
            var id = 1L
            if (addInboxFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.INBOX))
            }
            if (addOutboxFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.OUTBOX))
            }
            if (addSentFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.SENT))
            }
            if (addTrashFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.TRASH))
            }
            if (addArchiveFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.ARCHIVE))
            }
            if (addSpamFolder) {
                add(createRemoteFolder(id = id++, type = FolderType.SPAM))
            }
            if (regularFoldersSize > 0) {
                addAll(
                    elements = List(size = regularFoldersSize) { index ->
                        createRemoteFolder(id = id + index, type = FolderType.REGULAR)
                    },
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createTestSubject(
        accountUuid: String,
        folders: List<RemoteFolder>,
        exception: Exception? = null,
    ): GetAccountFolders {
        val folderRepository = mock<FolderRepository>()
        when {
            exception != null -> {
                `when`(folderRepository.getRemoteFolders(eq(accountUuid)))
                    .thenThrow(exception)
            }

            accountUuid == VALID_ACCOUNT_UUID -> {
                `when`(folderRepository.getRemoteFolders(eq(accountUuid)))
                    .thenReturn(folders)
            }

            accountUuid == INVALID_ACCOUNT_UUID ->
                `when`(folderRepository.getRemoteFolders(eq(accountUuid)))
                    .thenReturn(emptyList())
        }
        return GetAccountFolders(folderRepository, ioDispatcher = UnconfinedTestDispatcher())
    }
}
