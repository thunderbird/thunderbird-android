package com.fsck.k9.backend.jmap

import app.k9mail.backend.testing.InMemoryBackendStorage
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.fail
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FolderType
import net.thunderbird.core.common.exception.MessagingException
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import rs.ltt.jmap.client.JmapClient

class CommandRefreshFolderListTest {
    private val backendStorage = InMemoryBackendStorage()

    @Test
    fun sessionResourceWithAuthenticationError() {
        val command = createCommandRefreshFolderList(
            MockResponse().setResponseCode(401),
        )

        assertFailure {
            command.refreshFolderList()
        }.isInstanceOf<AuthenticationFailedException>()
    }

    @Test
    fun invalidSessionResource() {
        val command = createCommandRefreshFolderList(
            MockResponse().setBody("invalid"),
        )

        assertFailure {
            command.refreshFolderList()
        }.isInstanceOf<MessagingException>()
            .transform { it.isPermanentFailure }.isTrue()
    }

    @Test
    fun fetchMailboxes() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_get.json"),
        )

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder1")
        assertFolderPresent(serverId = "id_inbox", name = "Inbox", type = FolderType.INBOX)
        assertFolderPresent(serverId = "id_archive", name = "Archive", type = FolderType.ARCHIVE)
        assertFolderPresent(serverId = "id_drafts", name = "Drafts", type = FolderType.DRAFTS)
        assertFolderPresent(serverId = "id_sent", name = "Sent", type = FolderType.SENT)
        assertFolderPresent(serverId = "id_trash", name = "Trash", type = FolderType.TRASH)
        assertFolderPresent(serverId = "id_folder1", name = "folder1", type = FolderType.REGULAR)
        assertMailboxState("23")
    }

    @Test
    fun fetchMailboxUpdates() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes.json"),
        )
        createFoldersInBackendStorage(state = "23")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder2")
        assertFolderPresent(serverId = "id_inbox", name = "Inbox", type = FolderType.INBOX)
        assertFolderPresent(serverId = "id_archive", name = "Archive", type = FolderType.ARCHIVE)
        assertFolderPresent(serverId = "id_drafts", name = "Drafts", type = FolderType.DRAFTS)
        assertFolderPresent(serverId = "id_sent", name = "Sent", type = FolderType.SENT)
        assertFolderPresent(serverId = "id_trash", name = "Deleted messages", type = FolderType.TRASH)
        assertFolderPresent(serverId = "id_folder2", name = "folder2", type = FolderType.REGULAR)
        assertMailboxState("42")
    }

    @Test
    fun fetchMailboxUpdates_withHasMoreChanges() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_1.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_2.json"),
        )
        createFoldersInBackendStorage(state = "23")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder2")
        assertFolderPresent(serverId = "id_inbox", name = "Inbox", type = FolderType.INBOX)
        assertFolderPresent(serverId = "id_archive", name = "Archive", type = FolderType.ARCHIVE)
        assertFolderPresent(serverId = "id_drafts", name = "Drafts", type = FolderType.DRAFTS)
        assertFolderPresent(serverId = "id_sent", name = "Sent", type = FolderType.SENT)
        assertFolderPresent(serverId = "id_trash", name = "Deleted messages", type = FolderType.TRASH)
        assertFolderPresent(serverId = "id_folder2", name = "folder2", type = FolderType.REGULAR)
        assertMailboxState("42")
    }

    @Test
    fun fetchMailboxUpdates_withCannotCalculateChangesError() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_error_cannot_calculate_changes.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_get.json"),
        )
        setMailboxState("unknownToServer")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder1")
        assertFolderPresent(serverId = "id_inbox", name = "Inbox", type = FolderType.INBOX)
        assertFolderPresent(serverId = "id_archive", name = "Archive", type = FolderType.ARCHIVE)
        assertFolderPresent(serverId = "id_drafts", name = "Drafts", type = FolderType.DRAFTS)
        assertFolderPresent(serverId = "id_sent", name = "Sent", type = FolderType.SENT)
        assertFolderPresent(serverId = "id_trash", name = "Trash", type = FolderType.TRASH)
        assertFolderPresent(serverId = "id_folder1", name = "folder1", type = FolderType.REGULAR)
        assertMailboxState("23")
    }

    private fun createCommandRefreshFolderList(vararg mockResponses: MockResponse): CommandRefreshFolderList {
        val server = createMockWebServer(*mockResponses)
        return createCommandRefreshFolderList(server.url("/jmap/"))
    }

    private fun createCommandRefreshFolderList(
        baseUrl: HttpUrl,
        accountId: String = "test@example.com",
    ): CommandRefreshFolderList {
        val jmapClient = JmapClient("test", "test", baseUrl)
        return CommandRefreshFolderList(backendStorage, jmapClient, accountId)
    }

    @Suppress("SameParameterValue")
    private fun createFoldersInBackendStorage(state: String) {
        backendStorage.updateFolders {
            createFolder(serverId = "id_inbox", name = "Inbox", type = FolderType.INBOX)
            createFolder(serverId = "id_archive", name = "Archive", type = FolderType.ARCHIVE)
            createFolder(serverId = "id_drafts", name = "Drafts", type = FolderType.DRAFTS)
            createFolder(serverId = "id_sent", name = "Sent", type = FolderType.SENT)
            createFolder(serverId = "id_trash", name = "Trash", type = FolderType.TRASH)
            createFolder(serverId = "id_folder1", name = "folder1", type = FolderType.REGULAR)
        }
        setMailboxState(state)
    }

    private fun BackendFolderUpdater.createFolder(serverId: String, name: String, type: FolderType) {
        createFolders(listOf(FolderInfo(serverId = serverId, name = name, type = type)))
    }

    private fun setMailboxState(state: String) {
        backendStorage.setExtraString(name = "jmapState", value = state)
    }

    private fun assertFolderList(vararg folderServerIds: String) {
        assertThat(backendStorage.getFolderServerIds()).containsExactlyInAnyOrder(*folderServerIds)
    }

    private fun assertFolderPresent(serverId: String, name: String, type: FolderType) {
        val folder = backendStorage.folders[serverId] ?: fail("Expected folder '$serverId' in BackendStorage")

        assertThat(folder.name).isEqualTo(name)
        assertThat(folder.type).isEqualTo(type)
    }

    private fun assertMailboxState(expected: String) {
        assertThat(backendStorage.getExtraString("jmapState")).isEqualTo(expected)
    }
}
