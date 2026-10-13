package com.fsck.k9.backend.zohomail

import app.k9mail.backend.testing.InMemoryBackendFolder
import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class ZohoMailBackendSyncTest {
    private val storage = InMemoryBackendStorage()
    private val folder = InMemoryBackendFolder("Inbox", FolderType.INBOX).also {
        storage.folders[FOLDER_SERVER_ID] = it
    }

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
    }

    @Test
    fun `Zoho arrivals shift the fetch index only for Fetch more`() = runBlocking {
        folder.visibleLimit = 10
        folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, 10)
        folder.setFolderExtraNumber(KEY_OLDER_START, 11)
        folder.setMoreMessages(BackendFolder.MoreMessages.TRUE)
        folder.saveMessage(MimeMessage().apply { uid = "10" }, MessageDownloadState.FULL)

        val requestedPages = mutableListOf<Pair<Int, Int>>()
        val backend = createBackend { _, start, limit ->
            requestedPages += start to limit
            val messages = if (start == 1) {
                listOf(summary("12"), summary("11"), summary("10"))
            } else {
                (20L until 30L).map { summary(it.toString()) }
            }
            ZohoPage(messages, start, limit)
        }

        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(13L, folder.getFolderExtraNumber(KEY_OLDER_START))
        assertEquals(3, folder.getMessageServerIds().size)

        folder.visibleLimit = 20
        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(listOf(1 to 10, 1 to 10), requestedPages)
        assertEquals(13L, folder.getFolderExtraNumber(KEY_OLDER_START))

        folder.visibleLimit = 30
        backend.sync(FOLDER_SERVER_ID, syncConfig(fetchMoreMessagesRequested = true), NoopListener)

        assertEquals(listOf(1 to 10, 1 to 10, 13 to 10), requestedPages)
        assertEquals(23L, folder.getFolderExtraNumber(KEY_OLDER_START))
        assertEquals(13, folder.getMessageServerIds().size)
    }

    @Test
    fun `empty initial Zoho folder refresh imports newer arrivals but not older mail`() {
        var pageCount = 0
        var baselineTime = 0L
        val backend = createBackend { _, start, limit ->
            pageCount++
            if (pageCount == 1) {
                ZohoPage(emptyList(), start, limit)
            } else {
                ZohoPage(
                    listOf(
                        summary("new-arrival", baselineTime + 1),
                        summary("older-mail", baselineTime - 1),
                    ),
                    start,
                    limit,
                )
            }
        }
        folder.visibleLimit = 10

        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)
        baselineTime = checkNotNull(folder.getFolderExtraNumber(KEY_NEWEST_RECEIVED_TIME))
        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(setOf("new-arrival"), folder.getMessageServerIds())
    }

    @Test
    fun `refresh imports a newer message without received time and shifts the older index`() = runBlocking {
        folder.visibleLimit = 10
        folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, 10)
        folder.setFolderExtraNumber(KEY_OLDER_START, 2)
        folder.setFolderExtraNumber(KEY_NEWEST_RECEIVED_TIME, 100)
        folder.saveMessage(MimeMessage().apply { uid = "known" }, MessageDownloadState.FULL)
        val backend = createBackend { _, start, limit ->
            ZohoPage(listOf(summary("new-without-time", null), summary("known", 100)), start, limit)
        }

        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(setOf("known", "new-without-time"), folder.getMessageServerIds())
        assertEquals(3L, folder.getFolderExtraNumber(KEY_OLDER_START))
    }

    @Test
    fun `message Date uses Zoho raw header and envelope falls back to received time`() {
        val summary = summary("date-message", 1_710_915_486_000L)
        val details = ZohoMessage(
            id = summary.id,
            subject = summary.subject,
            content = "message body",
            from = null,
            to = null,
            sentTime = null,
            dateHeader = "Wed, 20 Mar 2024 11:48:06 +0530",
            messageIdHeader = null,
        )

        val message = ZohoMailMimeFactory.create(details, summary, emptyList())
        val envelope = ZohoMailMimeFactory.createEnvelope(summary)

        assertEquals("Wed, 20 Mar 2024 11:48:06 +0530", message.getHeader("Date").first())
        org.junit.Assert.assertNotNull(envelope.getHeader("Date"))
    }

    @Test
    fun `Zoho envelope preserves sender name and decodes recipient addresses`() {
        val summary = summary("address-message").copy(
            from = "sender@example.test",
            sender = "Sender Name",
            to = "&quot;Recipient Name&quot;&lt;recipient@example.test&gt;",
        )

        val testSubject = ZohoMailMimeFactory.createEnvelope(summary)

        assertEquals("Sender Name", testSubject.from.single().personal)
        val recipient = testSubject.getRecipients(RecipientType.TO).single()
        assertEquals("recipient@example.test", recipient.address)
        assertEquals("Recipient Name", recipient.personal)
    }

    @Test
    fun `full Zoho message uses the provider sender name with the sender address`() {
        val summary = summary("address-message")
        val details = ZohoMessage(
            id = summary.id,
            subject = summary.subject,
            content = "message body",
            from = "sender@example.test",
            to = "&quot;Recipient Name&quot;&lt;recipient@example.test&gt;",
            sentTime = null,
            messageIdHeader = null,
            sender = "Sender Name",
        )

        val testSubject = ZohoMailMimeFactory.create(details, summary, emptyList())

        assertEquals("sender@example.test", testSubject.from.single().address)
        assertEquals("Sender Name", testSubject.from.single().personal)
        val recipient = testSubject.getRecipients(RecipientType.TO).single()
        assertEquals("recipient@example.test", recipient.address)
        assertEquals("Recipient Name", recipient.personal)
    }

    @Test
    fun `partial MIME keeps attachment metadata while leaving attachment bytes lazy`() {
        val summary = summary("partial-message")
        val message = ZohoMailMimeFactory.createPartial(
            summary,
            listOf(
                ZohoAttachment("attachment-1", "report.pdf", "application/pdf", 1234, false, null),
                ZohoAttachment("attachment-2", "copy.pdf", null, null, false, null),
            ),
        )

        val parts = (message.body as Multipart).bodyParts
        val attachment = parts[1]

        assertEquals(3, parts.size)
        assertEquals("report.pdf", ZohoMailMimeFactory.fileName(attachment))
        assertEquals("application/pdf", attachment.mimeType)
        assertEquals("1234", MimeUtility.getHeaderParameter(attachment.disposition, "size"))
        assertEquals("attachment-1", ZohoMailMimeFactory.decodeReference(checkNotNull(attachment.serverExtra)))
        assertEquals(null, attachment.body)
        assertEquals("application/pdf", parts[2].mimeType)
        assertFalse(MessageExtractor.hasMissingParts(message))
    }

    @Test
    fun `remote partial message honors the configured size and avoids fetching message or attachment bytes`() {
        val requests = mutableListOf<Request>()
        val responses = mutableListOf(
            """{"data":[{"accountId":"123","emailAddress":["user@example.test"]}]}""",
            """{"data":[{"messageId":"51","size":100,"receivedtime":1,"subject":"Message"}]}""",
            """{
                "data": {
                    "attachments": [
                        {"attachmentId":"55", "attachmentName":"report.pdf", "contentType":"application/pdf", "size":1234}
                    ],
                    "inline": []
                }
            }""",
        )
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requests += request
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("scripted response")
                    .body(responses.removeAt(0).toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = ZohoMailApiClient(
            client = client,
            tokenProvider = TestTokenProvider,
            logger = TestLogger(),
            origin = "https://mail.zoho.com",
            mailboxAddress = "user@example.test",
        )
        val remoteFolderId = "1"
        storage.folders[remoteFolderId] = folder
        val testSubject = ZohoMailBackend(storage, api)

        testSubject.downloadMessage(syncConfig(), remoteFolderId, "51")

        assertEquals(
            listOf(
                "/api/accounts",
                "/api/accounts/123/messages/view",
                "/api/accounts/123/folders/1/messages/51/attachmentinfo",
            ),
            requests.map { it.url.encodedPath },
        )
        assertEquals(setOf(Flag.X_DOWNLOADED_PARTIAL), folder.getMessageFlags("51"))
    }

    @Test
    fun `initial visible limit larger than provider page size is fetched completely`() = runBlocking {
        folder.visibleLimit = 250
        val requestedPages = mutableListOf<Pair<Int, Int>>()
        val backend = createBackend { _, start, limit ->
            requestedPages += start to limit
            val messages = (start until start + limit).map { index -> summary("message-${index - 1}") }
            ZohoPage(messages, start, limit)
        }

        backend.sync(FOLDER_SERVER_ID, syncConfig(), NoopListener)

        assertEquals(listOf(1 to 200, 201 to 51), requestedPages)
        assertEquals(250, folder.getMessageServerIds().size)
        assertEquals(BackendFolder.MoreMessages.TRUE, folder.getMoreMessages())
    }

    @Test
    fun `newer refresh progress counts only saved items and does not double count indexes`() {
        folder.visibleLimit = 10
        folder.setFolderExtraNumber(KEY_VISIBLE_LIMIT, 10)
        runBlocking { folder.saveMessage(MimeMessage().apply { uid = "known" }, MessageDownloadState.FULL) }
        val headerProgress = mutableListOf<Pair<Int, Int>>()
        val syncProgress = mutableListOf<Pair<Int, Int>>()
        val listener = object : SyncListener by NoopListener {
            override fun syncHeadersProgress(folderServerId: String, completed: Int, total: Int) {
                headerProgress += completed to total
            }

            override fun syncProgress(folderServerId: String, completed: Int, total: Int) {
                syncProgress += completed to total
            }
        }
        val backend = createBackend { _, start, limit ->
            ZohoPage(listOf(summary("new-1"), summary("new-2"), summary("known")), start, limit)
        }

        backend.sync(FOLDER_SERVER_ID, syncConfig(), listener)

        assertEquals(listOf(1 to 3, 2 to 3), headerProgress)
        assertEquals(listOf(1 to 3, 2 to 3), syncProgress)
    }

    @Test
    fun `incoming-only Zoho account rejects sending before a REST request`() {
        val backend = createBackend(incomingOnly = true) { _, _, _ -> error("Unexpected message listing") }

        assertThrows(MessagingException::class.java) {
            backend.sendMessage(MimeMessage())
        }
    }

    private fun createBackend(
        incomingOnly: Boolean = false,
        messagePageLoader: (String, Int, Int) -> ZohoPage,
    ): ZohoMailBackend = ZohoMailBackend(
        storage = storage,
        api = ZohoMailApiClient(
            client = OkHttpClient(),
            tokenProvider = TestTokenProvider,
            logger = TestLogger(),
            origin = "https://mail.zoho.com",
            mailboxAddress = "user@example.test",
        ),
        incomingOnly = incomingOnly,
        messagePageLoader = messagePageLoader,
        attachmentInfoLoader = { _, _ -> emptyList() },
    )

    private fun summary(id: String, receivedTime: Long? = 1L) = ZohoSummary(
        id = id,
        receivedTime = receivedTime,
        subject = "Message",
        read = false,
        flagged = false,
        threadId = null,
        size = 100L,
        from = null,
        folderId = FOLDER_SERVER_ID,
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

    private companion object {
        const val FOLDER_SERVER_ID = "folder-id"
        const val KEY_VISIBLE_LIMIT = "zoho.visibleLimit"
        const val KEY_OLDER_START = "zoho.olderStart"
        const val KEY_NEWEST_RECEIVED_TIME = "zoho.newestReceivedTime"
    }
}

private object TestTokenProvider : OAuth2TokenProvider {
    override val usernames: Set<String> = emptySet()
    override fun getToken(timeoutMillis: Long): String = "unused-test-token"
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
