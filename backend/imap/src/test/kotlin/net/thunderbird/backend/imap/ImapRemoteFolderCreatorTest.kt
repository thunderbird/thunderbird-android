package net.thunderbird.backend.imap

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.backend.imap.TestImapFolder
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.store.imap.FolderListItem
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore
import kotlinx.coroutines.test.runTest
import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome.Error.FailedToCreateRemoteFolder
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.outcome.Outcome
import org.junit.Test

class ImapRemoteFolderCreatorTest {
    private val logger = TestLogger()

    @Test
    fun `when mustCreate true and folder exists, should return Error AlreadyExists`() = runTest {
        // Arrange
        val folderServerId = FolderServerId("New Folder")
        val fakeFolder = object : TestImapFolder(folderServerId.serverId) {
            override fun exists(): Boolean = true
        }
        val imapStore = FakeImapStore(fakeFolder)
        val sut = ImapRemoteFolderCreator(logger, imapStore)

        // Act
        val outcome = sut.create(folderServerId, mustCreate = true)

        // Assert
        assertAll {
            assertThat(outcome.isFailure).isTrue()
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<RemoteFolderCreationOutcome.Error>>()
                .prop("error") { it.error }
                .isEqualTo(RemoteFolderCreationOutcome.Error.AlreadyExists)
        }
    }

    @Test
    fun `when mustCreate false and folder exists, should return AlreadyExists`() = runTest {
        // Arrange
        val folderServerId = FolderServerId("New Folder")
        val fakeFolder = object : TestImapFolder(folderServerId.serverId) {
            override fun exists(): Boolean = true
        }
        val imapStore = FakeImapStore(fakeFolder)
        val sut = ImapRemoteFolderCreator(logger, imapStore)

        // Act
        val outcome = sut.create(folderServerId, mustCreate = false)

        // Assert
        assertAll {
            assertThat(outcome.isSuccess).isTrue()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<RemoteFolderCreationOutcome.Success>>()
                .prop("data") { it.data }
                .isEqualTo(RemoteFolderCreationOutcome.Success.AlreadyExists)
        }
    }

    @Test
    fun `when folder does not exist and creation succeeds, should return Created`() = runTest {
        // Arrange
        val folderServerId = FolderServerId("New Folder")
        val fakeFolder = object : TestImapFolder(folderServerId.serverId) {
            override fun exists(): Boolean = false
            override fun create(): Boolean = true
        }
        val imapStore = FakeImapStore(fakeFolder)
        val sut = ImapRemoteFolderCreator(logger, imapStore)

        // Act
        val outcome = sut.create(folderServerId, mustCreate = true)

        // Assert
        assertAll {
            assertThat(outcome.isSuccess).isTrue()
            assertThat(outcome)
                .isInstanceOf<Outcome.Success<RemoteFolderCreationOutcome.Success>>()
                .prop("data") { it.data }
                .isEqualTo(RemoteFolderCreationOutcome.Success.Created)
        }
    }

    @Test
    fun `when folder does not exist and creation fails, should return FailedToCreateRemoteFolder`() = runTest {
        // Arrange
        val folderServerId = FolderServerId("New Folder")
        val fakeFolder = object : TestImapFolder(folderServerId.serverId) {
            override fun exists(): Boolean = false
            override fun create(): Boolean = false
        }
        val imapStore = FakeImapStore(fakeFolder)
        val sut = ImapRemoteFolderCreator(logger, imapStore)

        // Act
        val outcome = sut.create(folderServerId, mustCreate = true)

        // Assert
        assertAll {
            assertThat(outcome.isFailure).isTrue()
            assertThat(outcome)
                .isInstanceOf<Outcome.Failure<FailedToCreateRemoteFolder>>()
                .prop("error") { it.error }
                .isInstanceOf<FailedToCreateRemoteFolder>()
                .prop(FailedToCreateRemoteFolder::reason)
                .isEqualTo("Failed to create folder on remote server.")
        }
    }
}

private class FakeImapStore(
    private val folder: TestImapFolder,
) : ImapStore {
    override fun checkSettings() {
        throw NotImplementedError("checkSettings not implemented")
    }

    override fun getFolder(name: String): ImapFolder = folder

    override fun getFolders(): List<FolderListItem> {
        throw NotImplementedError("getFolders not implemented")
    }

    override fun closeAllConnections() {
        throw NotImplementedError("closeAllConnections not implemented")
    }
}
