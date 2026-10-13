package com.fsck.k9.backend.graph

import app.k9mail.backend.testing.InMemoryBackendFolder
import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GraphBackendSyncTest {
    private val server = MockWebServer()
    private val storage = InMemoryBackendStorage()
    private val folder = storage.folders.getOrPut(FOLDER_SERVER_ID) {
        InMemoryBackendFolder("Inbox", FolderType.INBOX)
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
    fun `explicit Fetch more loads ten messages and preserves page overflow`() {
        server.start()
        folder.visibleLimit = 10
        val initialMessages = (0 until 10).joinToString(",") { index ->
            summary("id-$index", Instant.now().minus(index.toLong(), ChronoUnit.HOURS))
        }
        val olderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older-1")
        server.enqueue(MockResponse().setBody(pageWithNextLink(initialMessages, olderPageLink.toString())))
        val deltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=delta-1")
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("", deltaLink.toString())))
        repeat(10) { index ->
            server.enqueue(MockResponse().setBody(fullMessage("id-$index")))
        }

        val backend = createBackend()
        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(10, folder.getMessageServerIds().size)
        assertEquals(BackendFolder.MoreMessages.TRUE, folder.getMoreMessages())
        assertEquals(deltaLink.toString(), folder.getFolderExtraString("graphDeltaLink"))

        folder.visibleLimit += 25
        val secondOlderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older-2")
        val firstOlderPage = (10 until 25).joinToString(",") { index ->
            summary("id-$index", Instant.now().minus((index + 1).toLong(), ChronoUnit.DAYS))
        }
        server.enqueue(MockResponse().setBody(pageWithNextLink(firstOlderPage, secondOlderPageLink.toString())))
        repeat(10) { index -> server.enqueue(MockResponse().setBody(fullMessage("id-${index + 10}"))) }

        backend.sync(FOLDER_SERVER_ID, syncConfig(fetchMoreMessagesRequested = true), NoopListener)

        val expectedFirstBatch = (10 until 20).map { "id-$it" }.toSet()
        val initialIds = (0 until 10).map { "id-$it" }.toSet()
        assertEquals(20, folder.getMessageServerIds().size)
        assertEquals(expectedFirstBatch, folder.getMessageServerIds() - initialIds)
        assertEquals(secondOlderPageLink.toString(), folder.getFolderExtraString("graphOlderMessagesLink"))
        assertTrue(folder.getFolderExtraString("graphPendingOlderMessageIds") != null)
        assertEquals(BackendFolder.MoreMessages.TRUE, folder.getMoreMessages())

        folder.visibleLimit += 10
        val thirdOlderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older-3")
        val nextPage = (25 until 35).joinToString(",") { index -> summary("id-$index", Instant.now()) }
        server.enqueue(MockResponse().setBody(pageWithNextLink(nextPage, thirdOlderPageLink.toString())))
        repeat(10) { index -> server.enqueue(MockResponse().setBody(fullMessage("id-${index + 20}"))) }

        backend.sync(FOLDER_SERVER_ID, syncConfig(fetchMoreMessagesRequested = true), NoopListener)

        assertEquals(30, folder.getMessageServerIds().size)
        assertEquals(thirdOlderPageLink.toString(), folder.getFolderExtraString("graphOlderMessagesLink"))
        assertTrue(folder.getFolderExtraString("graphPendingOlderMessageIds") != null)
        assertEquals(BackendFolder.MoreMessages.TRUE, folder.getMoreMessages())
        assertEquals(34, server.requestCount)
    }

    @Test
    fun `ordinary refresh does not fetch an older page when visible limit grows`() {
        server.start()
        val oldDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=old")
        val olderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older")
        folder.setFolderExtraString("graphDeltaLink", oldDeltaLink.toString())
        folder.setFolderExtraString("graphVisibleLimit", "10")
        folder.setFolderExtraString("graphNewestReceivedTime", Instant.now().minus(1, ChronoUnit.DAYS).toString())
        folder.setFolderExtraString("graphOlderMessagesLink", olderPageLink.toString())
        folder.visibleLimit = 20
        val newDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=new")
        val olderMovedMessage = summary("older-moved", Instant.now().minus(2, ChronoUnit.DAYS))
        server.enqueue(MockResponse().setBody(pageWithDeltaLink(olderMovedMessage, newDeltaLink.toString())))

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("/messages/delta?token=old"))
        assertFalse(request.path!!.contains("skip=older"))
        assertEquals(olderPageLink.toString(), folder.getFolderExtraString("graphOlderMessagesLink"))
        assertTrue(folder.getMessageServerIds().isEmpty())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `legacy Graph folder is rebaselined from the visible window before applying delta changes`() = runBlocking {
        server.start()
        folder.visibleLimit = 1
        folder.setFolderExtraString("graphDeltaLink", server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=old").toString())
        folder.setFolderExtraString("graphVisibleLimit", "1")
        folder.saveMessage(MimeMessage().apply { uid = "baseline-message" }, MessageDownloadState.FULL)

        val baselineTime = Instant.now().minus(1, ChronoUnit.HOURS)
        val olderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older")
        server.enqueue(
            MockResponse().setBody(
                pageWithNextLink(summary("baseline-message", baselineTime), olderPageLink.toString()),
            ),
        )
        val nextDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=new")
        val olderMoved = summary("older-moved", baselineTime.minus(1, ChronoUnit.DAYS))
        val newerArrival = summary("newer-arrival", baselineTime.plus(1, ChronoUnit.MINUTES))
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("$olderMoved,$newerArrival", nextDeltaLink.toString())))
        server.enqueue(MockResponse().setBody(fullMessage("newer-arrival")))

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(setOf("baseline-message", "newer-arrival"), folder.getMessageServerIds())
        assertTrue("older-moved" !in folder.getMessageServerIds())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `initial sync stores an attachment-bearing message using metadata only`() {
        server.start()
        folder.visibleLimit = 1
        val messageSummary = summary("attachment-message", Instant.now(), hasAttachments = true)
        server.enqueue(MockResponse().setBody("""{"value": [$messageSummary]}"""))
        val deltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=attachment-delta")
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("", deltaLink.toString())))
        server.enqueue(MockResponse().setBody(fullMessage("attachment-message", hasAttachments = true)))
        server.enqueue(
            MockResponse().setBody(
                """
                    {"value":[{"id":"attachment-1","name":"notes.txt","contentType":"text/plain",
                    "size":4,"isInline":false,"@odata.type":"#microsoft.graph.fileAttachment"}]}
                """.trimIndent(),
            ),
        )

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertTrue("attachment-message" in folder.getMessageServerIds())
        assertEquals(4, server.requestCount)
        assertTrue(server.takeRequest().path!!.contains("/messages"))
        assertTrue(server.takeRequest().path!!.contains("/messages/delta"))
        assertTrue(server.takeRequest().path!!.contains("/messages/attachment-message"))
        val attachmentRequest = server.takeRequest()
        assertTrue(attachmentRequest.path!!.contains("/attachments"))
        assertEquals(
            "id,name,contentType,size,isInline",
            attachmentRequest.requestUrl!!.queryParameter("\$select"),
        )
    }

    @Test
    fun `initial sync uses the folder visible limit instead of a fixed count`() {
        server.start()
        folder.visibleLimit = 25
        val initialMessages = (0 until folder.visibleLimit).joinToString(",") { index ->
            summary("id-$index", Instant.now().minus(index.toLong(), ChronoUnit.HOURS))
        }
        val olderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older-1")
        server.enqueue(MockResponse().setBody(pageWithNextLink(initialMessages, olderPageLink.toString())))
        val deltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=delta-1")
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("", deltaLink.toString())))
        repeat(folder.visibleLimit) { index ->
            server.enqueue(MockResponse().setBody(fullMessage("id-$index")))
        }

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(25, folder.getMessageServerIds().size)
        assertEquals("25", server.takeRequest().requestUrl!!.queryParameter("\$top"))
        assertEquals(27, server.requestCount)
    }

    @Test
    fun `initial delta bootstrap queues arrivals beyond ten for the next refresh`() {
        server.start()
        folder.visibleLimit = 10
        val initialMessages = (0 until 10).joinToString(",") { index ->
            summary("id-$index", Instant.now().minus(index.toLong(), ChronoUnit.HOURS))
        }
        val olderPageLink = server.url("v1.0/me/mailFolders/folder-id/messages?skip=older-1")
        server.enqueue(MockResponse().setBody(pageWithNextLink(initialMessages, olderPageLink.toString())))

        val firstDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=first")
        val arrival = summary("arrived-during-bootstrap", Instant.now().plus(1, ChronoUnit.MINUTES))
        server.enqueue(MockResponse().setBody(pageWithDeltaLink(arrival, firstDeltaLink.toString())))
        (0 until 10).forEach { index -> server.enqueue(MockResponse().setBody(fullMessage("id-$index"))) }

        val backend = createBackend()
        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(10, folder.getMessageServerIds().size)
        assertTrue("id-9" in folder.getMessageServerIds())
        assertTrue(folder.getFolderExtraString("graphPendingMessageIds") != null)

        val secondDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=second")
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("", secondDeltaLink.toString())))
        server.enqueue(MockResponse().setBody(fullMessage("arrived-during-bootstrap")))
        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertTrue("arrived-during-bootstrap" in folder.getMessageServerIds())
        assertTrue("id-9" in folder.getMessageServerIds())
        assertEquals(null, folder.getFolderExtraString("graphPendingMessageIds"))
        assertEquals(14, server.requestCount)
    }

    @Test
    fun `normal refresh drains all delta pages without a message count cap`() {
        server.start()
        val oldDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=old")
        folder.setFolderExtraString("graphDeltaLink", oldDeltaLink.toString())
        folder.setFolderExtraString("graphVisibleLimit", folder.visibleLimit.toString())
        folder.setFolderExtraString("graphNewestReceivedTime", Instant.now().minus(1, ChronoUnit.HOURS).toString())
        val newPage = (0 until 12).joinToString(",") { index ->
            summary("new-$index", Instant.now().minus(index.toLong(), ChronoUnit.MINUTES))
        }
        val secondDeltaPageLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?skip=changes-2")
        server.enqueue(MockResponse().setBody(pageWithNextLink(newPage, secondDeltaPageLink.toString())))
        val newDeltaLink = server.url("v1.0/me/mailFolders/folder-id/messages/delta?token=new")
        server.enqueue(MockResponse().setBody(pageWithDeltaLink("", newDeltaLink.toString())))
        repeat(12) { index ->
            server.enqueue(MockResponse().setBody(fullMessage("new-$index")))
        }

        createBackend().sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(12, folder.getMessageServerIds().size)
        assertEquals(14, server.requestCount)
        assertTrue(folder.getFolderExtraString("graphDeltaLink")!!.contains("token=new"))
    }

    @Test
    fun `unsupported append is reported as permanent failure instead of crashing the controller`() {
        server.start()
        val error = assertThrows(MessagingException::class.java) {
            createBackend().uploadMessage(FOLDER_SERVER_ID, MimeMessage())
        }

        assertTrue(error.isPermanentFailure)
        assertEquals(0, server.requestCount)
    }

    private fun createBackend(): GraphBackend = GraphBackend(
        backendStorage = storage,
        api = GraphApiClient(
            OkHttpClient(),
            GraphBackendTestTokenProvider(),
            TestLogger(),
            server.url("v1.0/"),
        ),
    )

    private fun syncConfig(fetchMoreMessagesRequested: Boolean = false) = SyncConfig(
        expungePolicy = SyncConfig.ExpungePolicy.MANUALLY,
        earliestPollDate = null,
        syncRemoteDeletions = true,
        maximumAutoDownloadMessageSize = 1024 * 1024,
        defaultVisibleLimit = 25,
        syncFlags = setOf(Flag.SEEN, Flag.FLAGGED),
        fetchMoreMessagesRequested = fetchMoreMessagesRequested,
    )

    private fun summary(id: String, receivedAt: Instant, hasAttachments: Boolean = false): String = """
        {
          "id": "$id",
          "receivedDateTime": "$receivedAt",
          "isRead": false,
          "hasAttachments": $hasAttachments
        }
    """.trimIndent()

    private fun fullMessage(id: String, hasAttachments: Boolean = false): String = """
        {
          "id": "$id",
          "receivedDateTime": "${Instant.now()}",
          "sentDateTime": "${Instant.now()}",
          "internetMessageId": "<$id@example.test>",
          "subject": "$id",
          "from": {
            "emailAddress": {"name": "Sender", "address": "sender@example.test"}
          },
          "toRecipients": [
            {"emailAddress": {"name": "Recipient", "address": "recipient@example.test"}}
          ],
          "body": {"contentType": "text", "content": "Hello $id"},
          "isRead": false,
          "hasAttachments": $hasAttachments
        }
    """.trimIndent()

    private fun pageWithNextLink(values: String, nextLink: String): String = """
        {
          "value": [$values],
          "@odata.nextLink": "$nextLink"
        }
    """.trimIndent()

    private fun pageWithDeltaLink(values: String, deltaLink: String): String = """
        {
          "value": [$values],
          "@odata.deltaLink": "$deltaLink"
        }
    """.trimIndent()

    private companion object {
        const val FOLDER_SERVER_ID = "folder-id"
    }
}

private class GraphBackendTestTokenProvider : OAuth2TokenProvider {
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
        if (exception != null) throw AssertionError(message, exception)
        throw AssertionError(message)
    }

    override fun folderStatusChanged(folderServerId: String) = Unit
}
