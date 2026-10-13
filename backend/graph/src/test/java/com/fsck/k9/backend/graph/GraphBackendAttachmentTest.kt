package com.fsck.k9.backend.graph

import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.DefaultBodyFactory
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMultipart
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import java.io.File
import java.io.InputStream
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.legacy.logging.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GraphBackendAttachmentTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
        Log.logger = TestLogger()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `attachment fetch reuses the imported server extra key and accepts matching HTTP Content-Length`() {
        server.enqueue(MockResponse().setBody("data"))
        server.start()
        val part = createAttachmentPart(size = 4)
        val savedLookupKey = part.serverExtra

        createBackend().fetchPart(FOLDER_SERVER_ID, MESSAGE_SERVER_ID, part, DefaultBodyFactory())

        assertEquals(savedLookupKey, part.serverExtra)
        assertEquals("attachment-1", GraphMessageMimeFactory.decodeAttachmentId(part.serverExtra))
        assertEquals("data", part.body.inputStream.bufferedReader().use { it.readText() })
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("/messages/$MESSAGE_SERVER_ID/attachments/attachment-1/"))
    }

    @Test
    fun `metadata size over-report does not reject the streamed attachment`() {
        server.enqueue(MockResponse().setChunkedBody("data", 2))
        server.start()
        val logger = TestLogger()
        val part = createAttachmentPart(size = 308)

        createBackend(logger).fetchPart(FOLDER_SERVER_ID, MESSAGE_SERVER_ID, part, DefaultBodyFactory())

        assertEquals("data", part.body.inputStream.bufferedReader().use { it.readText() })
        val discrepancyLog = logger.events.single { it.message.contains("metadataBytes=") }.message
        assertTrue(discrepancyLog.contains("metadataBytes=308 downloadedBytes=4"))
        assertFalse(discrepancyLog.contains(MESSAGE_SERVER_ID))
        assertFalse(discrepancyLog.contains("attachment-1"))
        assertFalse(discrepancyLog.contains("notes.txt"))
        assertNotNull(server.takeRequest())
    }

    @Test
    fun `known HTTP Content-Length mismatch raises and releases the temporary body`() {
        server.enqueue(MockResponse().setBody("abc"))
        server.start()
        val part = createAttachmentPart(size = 3)
        val savedLookupKey = part.serverExtra
        val bodyFactory = TrackingBodyFactory()

        try {
            createBackend(okHttpClient = clientWithDeclaredContentLength(4))
                .fetchPart(FOLDER_SERVER_ID, MESSAGE_SERVER_ID, part, bodyFactory)
            throw AssertionError("Expected Content-Length mismatch")
        } catch (e: MessagingException) {
            assertTrue(e.message!!.contains("Content-Length mismatch"))
        }

        assertEquals(savedLookupKey, part.serverExtra)
        assertNull(part.body)
        val temporaryBody = bodyFactory.body as BinaryTempFileBody
        assertFalse(temporaryBody.file.exists())
        assertNotNull(server.takeRequest())
    }

    private fun createBackend(
        logger: TestLogger = TestLogger(),
        okHttpClient: OkHttpClient = OkHttpClient(),
    ): GraphBackend = GraphBackend(
        backendStorage = InMemoryBackendStorage(),
        api = GraphApiClient(
            okHttpClient = okHttpClient,
            tokenProvider = GraphAttachmentTestTokenProvider(),
            logger = logger,
            baseUrl = server.url("v1.0/"),
        ),
    )

    private fun clientWithDeclaredContentLength(contentLength: Long): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val originalBody = response.body
            response.newBuilder()
                .header("Content-Length", contentLength.toString())
                .body(object : ResponseBody() {
                    override fun contentType() = originalBody.contentType()
                    override fun contentLength() = contentLength
                    override fun source(): BufferedSource = originalBody.source()
                })
                .build()
        }
        .build()

    private fun createAttachmentPart(size: Long): Part {
        val message = GraphMessage(
            id = MESSAGE_SERVER_ID,
            receivedDateTime = null,
            sentDateTime = null,
            lastModifiedDateTime = null,
            subject = null,
            internetMessageId = null,
            from = null,
            sender = null,
            toRecipients = emptyList(),
            ccRecipients = emptyList(),
            bccRecipients = emptyList(),
            replyTo = emptyList(),
            bodyContent = "Message body",
            bodyContentType = "text",
            bodyPreview = null,
            isRead = false,
            isFlagged = false,
            hasAttachments = true,
            removed = false,
        )
        val attachment = GraphAttachment(
            id = "attachment-1",
            name = "notes.txt",
            contentType = "text/plain",
            size = size,
            isInline = false,
            contentId = null,
            type = "#microsoft.graph.fileAttachment",
        )
        return (GraphMessageMimeFactory.create(message, listOf(attachment)).body as MimeMultipart).getBodyPart(1)
    }

    private class TrackingBodyFactory : BodyFactory {
        var body: Body? = null

        override fun createBody(
            contentTransferEncoding: String,
            contentType: String,
            inputStream: InputStream,
        ): Body {
            return DefaultBodyFactory()
                .createBody(contentTransferEncoding, contentType, inputStream)
                .also { body = it }
        }
    }

    private companion object {
        const val FOLDER_SERVER_ID = "folder-1"
        const val MESSAGE_SERVER_ID = "message-1"
    }
}

private class GraphAttachmentTestTokenProvider : OAuth2TokenProvider {
    override val usernames: Set<String> = emptySet()

    override fun getToken(timeoutMillis: Long): String = "test-token"

    override fun invalidateToken() = Unit
}
