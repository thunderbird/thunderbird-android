package com.fsck.k9.backend.gmailapi

import app.k9mail.backend.testing.InMemoryBackendFolder
import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailBackendSyncTest {
    private val server = MockWebServer()
    private val storage = InMemoryBackendStorage()
    private val folder = InMemoryBackendFolder("Inbox", FolderType.INBOX).also {
        storage.folders[FOLDER_SERVER_ID] = it
    }

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refresh imports only newer history additions and never backfills older label changes`() = runBlocking {
        folder.visibleLimit = 20
        folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, 10)
        folder.setFolderExtraString(EXTRA_HISTORY_ID, "cursor-1")
        folder.saveMessage(MimeMessage().apply { uid = "deleted-message" }, MessageDownloadState.FULL)
        server.enqueue(
            MockResponse().setBody(
                """{"history":[{"id":"20","messagesAdded":[{"message":{"id":"new-message"}}],"labelsAdded":[{"message":{"id":"older-labelled-message"},"labelIds":["INBOX"]}],"messagesDeleted":[{"message":{"id":"deleted-message"}}]}],"historyId":"cursor-2"}""",
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """{"id":"new-message","threadId":"thread-new","labelIds":["INBOX","UNREAD"],"sizeEstimate":100,"internalDate":"1700000000000","payload":{"mimeType":"text/plain","headers":[{"name":"Subject","value":"New message"}]}}""",
            ),
        )

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(setOf("new-message"), folder.getMessageServerIds())
        assertEquals("cursor-2", folder.getFolderExtraString(EXTRA_HISTORY_ID))
        assertEquals(2, server.requestCount)
        assertTrue(server.takeRequest().path!!.contains("/history"))
        assertTrue(server.takeRequest().path!!.contains("/messages/new-message"))
    }

    @Test
    fun `explicit Load more requests exactly ten Gmail messages and does not advance the history cursor`() {
        folder.visibleLimit = 20
        folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, 10)
        folder.setFolderExtraString(EXTRA_HISTORY_ID, "cursor-1")
        folder.setFolderExtraString(EXTRA_OLDER_PAGE_TOKEN, "older-page")
        val references = (0 until 10).joinToString(",") { index -> "{\"id\":\"older-$index\"}" }
        server.enqueue(MockResponse().setBody("""{"messages":[$references],"nextPageToken":"older-next"}"""))
        repeat(10) { index ->
            server.enqueue(
                MockResponse().setBody(
                    """{"id":"older-$index","labelIds":["INBOX"],"sizeEstimate":100,"internalDate":"1700000000000","payload":{"mimeType":"text/plain","headers":[{"name":"Subject","value":"Older"}]}}""",
                ),
            )
        }

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(fetchMoreMessagesRequested = true), NoopListener)

        assertEquals(10, folder.getMessageServerIds().size)
        assertEquals("cursor-1", folder.getFolderExtraString(EXTRA_HISTORY_ID))
        assertEquals("older-next", folder.getFolderExtraString(EXTRA_OLDER_PAGE_TOKEN))
        val loadMoreRequest = server.takeRequest().requestUrl!!
        assertEquals("10", loadMoreRequest.queryParameter("maxResults"))
        assertEquals("older-page", loadMoreRequest.queryParameter("pageToken"))
        assertEquals(11, server.requestCount)
    }

    @Test
    fun `initial sync requests the visible limit and stores the newest received-time baseline`() {
        val baselineTimeMillis = 1_700_000_000_000L
        folder.visibleLimit = 10
        server.enqueue(MockResponse().setBody("""{"historyId":"initial-cursor"}"""))
        val references = (0 until 10).joinToString(",") { index -> """{"id":"initial-$index"}""" }
        server.enqueue(MockResponse().setBody("""{"messages":[$references],"nextPageToken":"older-page"}"""))
        repeat(10) { index ->
            server.enqueue(MockResponse().setBody(metadataMessage("initial-$index", baselineTimeMillis + index)))
        }

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(10, folder.getMessageServerIds().size)
        assertEquals("initial-cursor", folder.getFolderExtraString(EXTRA_HISTORY_ID))
        assertEquals("older-page", folder.getFolderExtraString(EXTRA_OLDER_PAGE_TOKEN))
        assertEquals(baselineTimeMillis + 9, folder.getFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE))
        assertTrue(server.takeRequest().path!!.endsWith("/profile"))
        assertEquals("10", server.takeRequest().requestUrl!!.queryParameter("maxResults"))
        assertEquals(12, server.requestCount)
    }

    @Test
    fun `expired history recovery searches after the baseline and filters same-second older mail`() {
        val baselineTimeMillis = 1_700_000_000_500L
        folder.visibleLimit = 20
        folder.setFolderExtraNumber(EXTRA_VISIBLE_LIMIT, 10)
        folder.setFolderExtraString(EXTRA_HISTORY_ID, "expired-cursor")
        folder.setFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE, baselineTimeMillis)
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":{"errors":[{"reason":"notFound"}]}}"""))
        server.enqueue(MockResponse().setBody("""{"historyId":"cursor-2"}"""))
        server.enqueue(MockResponse().setBody("""{"messages":[{"id":"newer-message"},{"id":"same-second-older"}]}"""))
        server.enqueue(MockResponse().setBody(metadataMessage("newer-message", 1_700_000_002_000L)))
        server.enqueue(MockResponse().setBody(metadataMessage("same-second-older", 1_700_000_000_400L)))

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(setOf("newer-message"), folder.getMessageServerIds())
        assertEquals("cursor-2", folder.getFolderExtraString(EXTRA_HISTORY_ID))
        assertEquals(1_700_000_002_000L, folder.getFolderExtraNumber(EXTRA_NEWEST_INTERNAL_DATE))
        assertTrue(server.takeRequest().path!!.contains("/history"))
        assertTrue(server.takeRequest().path!!.endsWith("/profile"))
        val recoveryRequest = server.takeRequest()
        assertEquals("after:1700000000", recoveryRequest.requestUrl!!.queryParameter("q"))
        assertEquals(5, server.requestCount)
    }

    private fun createBackend() = GmailBackend(
        storage = storage,
        api = GmailApiClient(
            client = OkHttpClient(),
            tokenProvider = TestTokenProvider,
            logger = TestLogger(),
            baseUrl = server.url("gmail/v1/users"),
        ),
    )

    private fun syncConfig(fetchMoreMessagesRequested: Boolean = false) = SyncConfig(
        expungePolicy = SyncConfig.ExpungePolicy.MANUALLY,
        earliestPollDate = null,
        syncRemoteDeletions = true,
        maximumAutoDownloadMessageSize = 1,
        defaultVisibleLimit = 10,
        syncFlags = emptySet<Flag>(),
        fetchMoreMessagesRequested = fetchMoreMessagesRequested,
    )

    private fun metadataMessage(id: String, internalDateMillis: Long) =
        """{"id":"$id","labelIds":["INBOX"],"sizeEstimate":100,"internalDate":"$internalDateMillis","payload":{"mimeType":"text/plain","headers":[{"name":"Subject","value":"Message"}]}}"""

    private companion object {
        const val FOLDER_SERVER_ID = "INBOX"
        const val EXTRA_VISIBLE_LIMIT = "gmail.visibleLimit"
        const val EXTRA_HISTORY_ID = "gmail.historyId"
        const val EXTRA_NEWEST_INTERNAL_DATE = "gmail.newestInternalDate"
        const val EXTRA_OLDER_PAGE_TOKEN = "gmail.olderPageToken"
    }
}

private object TestTokenProvider : OAuth2TokenProvider {
    override val usernames: Set<String> = emptySet()
    override fun getToken(timeoutMillis: Long): String = "test-token"
    override fun invalidateToken() = Unit
}

private object NoopListener : SyncListener {
    override fun syncStarted(folderServerId: String) = Unit
    override fun syncAuthenticationSuccess() = Unit
    override fun syncHeadersStarted(folderServerId: String) = Unit
    override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override fun syncHeadersFinished(folderServerId: String, totalMessagesInMailbox: Int, numNewMessages: Int) = Unit
    override fun syncProgress(folderServerId: String, completed: Int, total: Int) = Unit
    override fun syncNewMessage(folderServerId: String, messageServerId: String, isOldMessage: Boolean) = Unit
    override fun syncRemovedMessage(folderServerId: String, messageServerId: String) = Unit
    override fun syncFlagChanged(folderServerId: String, messageServerId: String) = Unit
    override fun syncFinished(folderServerId: String) = Unit
    override fun syncFailed(folderServerId: String, message: String, exception: Exception?) {
        throw AssertionError(message, exception)
    }
    override fun folderStatusChanged(folderServerId: String) = Unit
}
