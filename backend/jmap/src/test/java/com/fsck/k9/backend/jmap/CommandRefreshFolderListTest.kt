package com.fsck.k9.backend.jmap

import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessagingException
import junit.framework.AssertionFailedError
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import rs.ltt.jmap.client.JmapClient

class CommandRefreshFolderListTest {
    private val backendStorage = InMemoryBackendStorage()

    @Test
    fun sessionResourceWithAuthenticationError() {
        val command = createCommandRefreshFolderList(
            MockResponse().setResponseCode(401)
        )

        try {
            command.refreshFolderList()
            fail("Expected exception")
        } catch (e: AuthenticationFailedException) {
        }
    }

    @Test
    fun invalidSessionResource() {
        val command = createCommandRefreshFolderList(
            MockResponse().setBody("invalid")
        )

        try {
            command.refreshFolderList()
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertTrue(e.isPermanentFailure)
        }
    }

    @Test
    fun fetchMailboxes() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_get.json")
        )

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder1")
        assertFolderPresent("id_inbox", "Inbox", FolderType.INBOX)
        assertFolderPresent("id_archive", "Archive", FolderType.ARCHIVE)
        assertFolderPresent("id_drafts", "Drafts", FolderType.DRAFTS)
        assertFolderPresent("id_sent", "Sent", FolderType.SENT)
        assertFolderPresent("id_trash", "Trash", FolderType.TRASH)
        assertFolderPresent("id_folder1", "folder1", FolderType.REGULAR)
        assertMailboxState("23")
    }

    @Test
    fun fetchMailboxUpdates() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes.json")
        )
        createFoldersInBackendStorage(state = "23")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder2")
        assertFolderPresent("id_inbox", "Inbox", FolderType.INBOX)
        assertFolderPresent("id_archive", "Archive", FolderType.ARCHIVE)
        assertFolderPresent("id_drafts", "Drafts", FolderType.DRAFTS)
        assertFolderPresent("id_sent", "Sent", FolderType.SENT)
        assertFolderPresent("id_trash", "Deleted messages", FolderType.TRASH)
        assertFolderPresent("id_folder2", "folder2", FolderType.REGULAR)
        assertMailboxState("42")
    }

    @Test
    fun fetchMailboxUpdates_withHasMoreChanges() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_1.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_2.json")
        )
        createFoldersInBackendStorage(state = "23")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder2")
        assertFolderPresent("id_inbox", "Inbox", FolderType.INBOX)
        assertFolderPresent("id_archive", "Archive", FolderType.ARCHIVE)
        assertFolderPresent("id_drafts", "Drafts", FolderType.DRAFTS)
        assertFolderPresent("id_sent", "Sent", FolderType.SENT)
        assertFolderPresent("id_trash", "Deleted messages", FolderType.TRASH)
        assertFolderPresent("id_folder2", "folder2", FolderType.REGULAR)
        assertMailboxState("42")
    }

    @Test
    fun fetchMailboxUpdates_withCannotCalculateChangesError() {
        val command = createCommandRefreshFolderList(
            responseBodyFromResource("/jmap_responses/session/valid_session.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_changes_error_cannot_calculate_changes.json"),
            responseBodyFromResource("/jmap_responses/mailbox/mailbox_get.json")
        )
        setMailboxState("unknownToServer")

        command.refreshFolderList()

        assertFolderList("id_inbox", "id_archive", "id_drafts", "id_sent", "id_trash", "id_folder1")
        assertFolderPresent("id_inbox", "Inbox", FolderType.INBOX)
        assertFolderPresent("id_archive", "Archive", FolderType.ARCHIVE)
        assertFolderPresent("id_drafts", "Drafts", FolderType.DRAFTS)
        assertFolderPresent("id_sent", "Sent", FolderType.SENT)
        assertFolderPresent("id_trash", "Trash", FolderType.TRASH)
        assertFolderPresent("id_folder1", "folder1", FolderType.REGULAR)
        assertMailboxState("23")
    }

    private fun createCommandRefreshFolderList(vararg mockResponses: MockResponse): CommandRefreshFolderList {
        val server = createMockWebServer(*mockResponses)
        return createCommandRefreshFolderList(server.url("/jmap/"))
    }

    private fun createCommandRefreshFolderList(
        baseUrl: HttpUrl,
        accountId: String = "test@example.com"
    ): CommandRefreshFolderList {
        val jmapClient = JmapClient("test", "test", baseUrl)
        return CommandRefreshFolderList(backendStorage, jmapClient, accountId)
    }

    @Suppress("SameParameterValue")
    private fun createFoldersInBackendStorage(state: String) {
        backendStorage.updateFolders {
            createFolder("id_inbox", "Inbox", FolderType.INBOX)
            createFolder("id_archive", "Archive", FolderType.ARCHIVE)
            createFolder("id_drafts", "Drafts", FolderType.DRAFTS)
            createFolder("id_sent", "Sent", FolderType.SENT)
            createFolder("id_trash", "Trash", FolderType.TRASH)
            createFolder("id_folder1", "folder1", FolderType.REGULAR)
        }
        setMailboxState(state)
    }

    private fun BackendFolderUpdater.createFolder(serverId: String, name: String, type: FolderType) {
        createFolders(listOf(FolderInfo(serverId, name, type)))
    }

    private fun setMailboxState(state: String) {
        backendStorage.setExtraString("jmapState", state)
    }

    private fun assertFolderList(vararg folderServerIds: String) {
        assertEquals(folderServerIds.toSet(), backendStorage.getFolderServerIds().toSet())
    }

    private fun assertFolderPresent(serverId: String, name: String, type: FolderType) {
        val folder = backendStorage.folders[serverId]
            ?: throw AssertionFailedError("Expected folder '$serverId' in BackendStorage")

        assertEquals(name, folder.name)
        assertEquals(type, folder.type)
    }

    private fun assertMailboxState(expected: String) {
        assertEquals(expected, backendStorage.getExtraString("jmapState"))
    }
}
