package com.fsck.k9.backend.graph

import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMessage
import net.thunderbird.core.logging.testing.TestLogger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.net.SocketTimeoutException
import java.util.Base64
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphApiClientTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `attachment metadata request selects only common properties and omits content bytes`() {
        val attachmentResponse = """
            {
              "value": [
                {
                  "id": "att-1",
                  "name": "notes.txt",
                  "contentType": "text/plain",
                  "size": 4,
                  "isInline": false,
                  "@odata.type": "#microsoft.graph.fileAttachment"
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(attachmentResponse))
        server.start()
        val client = createClient()

        val attachments = client.listAttachments("message-1")

        assertEquals("att-1", attachments.single().id)
        assertEquals(4L, attachments.single().size)
        assertEquals("#microsoft.graph.fileAttachment", attachments.single().type)
        val request = server.takeRequest()
        assertEquals("id,name,contentType,size,isInline", request.requestUrl!!.queryParameter("\$select"))
        assertTrue(!request.path!!.contains("contentBytes"))
        assertEquals("Bearer token-1", request.getHeader("Authorization"))
    }

    @Test
    fun `inline file attachment content ID is fetched from its typed resource without content bytes`() {
        server.enqueue(
            MockResponse().setBody(
                """
                    {"value":[{"id":"inline-1","name":"image.png","contentType":"image/png",
                    "size":4,"isInline":true,"@odata.type":"#microsoft.graph.fileAttachment"}]}
                """.trimIndent(),
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """
                    {"id":"inline-1","contentId":"image-content-id",
                    "@odata.type":"#microsoft.graph.fileAttachment"}
                """.trimIndent(),
            ),
        )
        server.start()

        val attachment = createClient().listAttachments("message-1").single()

        assertEquals("image-content-id", attachment.contentId)
        val listRequest = server.takeRequest()
        assertEquals("id,name,contentType,size,isInline", listRequest.requestUrl!!.queryParameter("\$select"))
        val detailRequest = server.takeRequest()
        assertEquals("id,contentId", detailRequest.requestUrl!!.queryParameter("\$select"))
        assertTrue(detailRequest.path!!.contains("/attachments/inline-1"))
        assertTrue(!detailRequest.path!!.contains("contentBytes"))
    }

    @Test
    fun `attachment metadata paging follows opaque continuation links without fetching bytes`() {
        server.start()
        val nextLink = server.url("v1.0/me/messages/message-1/attachments?${'$'}skiptoken=next-page")
        server.enqueue(
            MockResponse().setBody(
                """{"value":[{"id":"attachment-1","name":"one.txt","isInline":false}],"@odata.nextLink":"$nextLink"}""",
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """{"value":[{"id":"attachment-2","name":"two.txt","isInline":false}]}""",
            ),
        )

        val attachments = createClient().listAttachments("message-1")

        assertEquals(listOf("attachment-1", "attachment-2"), attachments.map { it.id })
        val firstRequest = server.takeRequest()
        assertEquals("id,name,contentType,size,isInline", firstRequest.requestUrl!!.queryParameter("\$select"))
        assertTrue(!firstRequest.path!!.contains("contentBytes"))
        val secondRequest = server.takeRequest()
        assertEquals("next-page", secondRequest.requestUrl!!.queryParameter("\$skiptoken"))
        assertTrue(!secondRequest.path!!.contains("contentBytes"))
    }

    @Test
    fun `continuation links outside the Graph origin are rejected before a request`() {
        server.start()
        val client = createClient()

        try {
            client.getMessagePage("https://attacker.example/v1.0/me/messages?skip=secret")
            throw AssertionError("Expected unsafe continuation URL to be rejected")
        } catch (e: GraphApiException) {
            assertEquals(400, e.statusCode)
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `unauthorized response refreshes OAuth token once`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val deltaLink = "https://graph.microsoft.com/v1.0/me/messages/delta?deltatoken=x"
        server.enqueue(MockResponse().setBody(pageWithDeltaLink(deltaLink)))
        server.start()
        val tokens = GraphApiTestTokenProvider()
        val client = createClient(tokens)

        val page = client.getMessageDeltaPage("folder-1", cutoff = null)

        assertTrue(page.values.isEmpty())
        assertEquals(1, tokens.invalidations)
        assertEquals("Bearer token-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer token-2", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `safe GET retries a throttled response honoring Retry-After`() {
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody("""{"value":[]}"""))
        server.start()

        val page = createClient().getLatestMessages("folder-1", limit = 10)

        assertTrue(page.values.isEmpty())
        assertEquals(2, server.requestCount)
        assertEquals("GET", server.takeRequest().method)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `transient service failure does not replay a state-changing request`() {
        server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0").setBody("{}"))
        server.start()

        assertThrows(GraphApiException::class.java) { createClient().setMessageRead("message-1", isRead = true) }

        assertEquals(1, server.requestCount)
        assertEquals("PATCH", server.takeRequest().method)
    }

    @Test
    fun `protocol logs include Graph status correlation and returned message IDs but omit credentials and content`() {
        val logger = TestLogger()
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .addHeader("request-id", "graph-request-unauthorized")
                .setBody("""{"error":{"code":"InvalidAuthenticationToken","message":"private error detail"}}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("request-id", "graph-request-success")
                .addHeader("client-request-id", "echoed-client-request")
                .setBody(
                    """
                        {
                          "value": [
                            {"id":"sent-message-1","subject":"private subject"},
                            {"id":"sent-message-2"}
                          ],
                          "@odata.nextLink": "https://graph.microsoft.com/v1.0/me/messages?${'$'}skiptoken=private-token"
                        }
                    """.trimIndent(),
                ),
        )
        server.start()

        val page = createClient(logger = logger).getLatestMessages("sent-folder", limit = 10)
        val logMessages = logger.events.joinToString("\n") { it.message }

        assertEquals(listOf("sent-message-1", "sent-message-2"), page.values.map { it.id })
        assertTrue(logMessages.contains("status=401"))
        assertTrue(logMessages.contains("errorCode=InvalidAuthenticationToken"))
        assertTrue(logMessages.contains("requestId=graph-request-unauthorized"))
        assertTrue(logMessages.contains("status=200"))
        assertTrue(logMessages.contains("requestId=graph-request-success"))
        assertTrue(logMessages.contains("echoedClientRequestId=echoed-client-request"))
        assertTrue(logMessages.contains("messageIds=[sent-message-1, sent-message-2]"))
        assertTrue(logMessages.contains("itemCount=2 parsedCount=2"))
        assertTrue(!logMessages.contains("token-1"))
        assertTrue(!logMessages.contains("private subject"))
        assertTrue(!logMessages.contains("private error detail"))
        assertTrue(!logMessages.contains("private-token"))

        val firstRequest = server.takeRequest()
        assertTrue(firstRequest.getHeader("client-request-id")!!.isNotBlank())
        assertEquals("true", firstRequest.getHeader("return-client-request-id"))
        val retriedRequest = server.takeRequest()
        assertTrue(retriedRequest.getHeader("client-request-id")!!.isNotBlank())
        assertTrue(firstRequest.getHeader("client-request-id") != retriedRequest.getHeader("client-request-id"))
    }

    @Test
    fun `sendMessage posts base64 MIME to Graph sendMail endpoint`() {
        server.enqueue(MockResponse().setResponseCode(202))
        server.start()
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
        val rawMime = "From: sender@example.com\r\n" +
            "To: receiver@example.com\r\n" +
            "Subject: Graph send test\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
            "hello from Graph\r\n"
        val message = MimeMessage.parseMimeMessage(ByteArrayInputStream(rawMime.toByteArray(UTF_8)), false)

        createClient().sendMessage(message)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1.0/me/sendMail", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("text/plain"))
        assertEquals("Bearer token-1", request.getHeader("Authorization"))
        val decodedMime = String(Base64.getDecoder().decode(request.body.readUtf8()), UTF_8)
        assertTrue(decodedMime.contains("Subject: Graph send test"))
        assertTrue(decodedMime.contains("hello from Graph"))
    }

    @Test
    fun `sendMessage does not implicitly replay a 503 response`() {
        server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0").setBody("{}"))
        server.start()
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))

        assertThrows(GraphApiException::class.java) { createClient().sendMessage(createMessage()) }

        assertEquals(1, server.requestCount)
        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun `sendMessage reports no-response I O failures as ambiguous permanent failures`() {
        server.start()
        BinaryTempFileBody.setTempDirectory(File(System.getProperty("java.io.tmpdir")))
        val message = createMessage()
        val client = GraphApiClient(
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor { throw SocketTimeoutException("synthetic timeout") }
                .build(),
            tokenProvider = GraphApiTestTokenProvider(),
            logger = TestLogger(),
            baseUrl = server.url("v1.0/"),
        )

        try {
            client.sendMessage(message)
            throw AssertionError("Expected an unknown send outcome")
        } catch (e: GraphSendOutcomeUnknownException) {
            assertTrue(e.isPermanentFailure)
            assertTrue(e.message!!.contains("Check Sent Items before retrying"))
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `moveMessage posts destination and returns the moved server ID`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"moved-id"}"""))
        server.start()

        val messageId = createClient().moveMessage("source-id", "target-folder")

        val request = server.takeRequest()
        assertEquals("moved-id", messageId)
        assertEquals("POST", request.method)
        assertEquals("/v1.0/me/messages/source-id/move", request.path)
        assertTrue(request.body.readUtf8().contains("\"destinationId\":\"target-folder\""))
    }

    @Test
    fun `copyMessage posts destination and returns the copied server ID`() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"copy-id"}"""))
        server.start()

        val messageId = createClient().copyMessage("source-id", "target-folder")

        val request = server.takeRequest()
        assertEquals("copy-id", messageId)
        assertEquals("POST", request.method)
        assertEquals("/v1.0/me/messages/source-id/copy", request.path)
        assertTrue(request.body.readUtf8().contains("\"destinationId\":\"target-folder\""))
    }

    @Test
    fun `flag updates use Graph message properties and deletion uses DELETE`() {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(204))
        server.start()
        val client = createClient()

        client.setMessageRead("message-id", true)
        client.setMessageFlagged("message-id", true)
        client.deleteMessage("message-id")

        val readRequest = server.takeRequest()
        assertEquals("PATCH", readRequest.method)
        assertTrue(readRequest.body.readUtf8().contains("\"isRead\":true"))
        val flagRequest = server.takeRequest()
        assertEquals("PATCH", flagRequest.method)
        assertTrue(flagRequest.body.readUtf8().contains("\"flagStatus\":\"flagged\""))
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `listMessageIds returns unread IDs from the folder`() {
        server.enqueue(
            MockResponse().setBody(
                """{"value":[{"id":"unread-1","isRead":false},{"id":"read-1","isRead":true}]}""",
            ),
        )
        server.start()

        val messageIds = createClient().listMessageIds("folder-id", unreadOnly = true)

        assertEquals(listOf("unread-1"), messageIds)
        assertTrue(server.takeRequest().path!!.contains("isRead%20eq%20false"))
    }

    private fun createMessage(): MimeMessage {
        val rawMime = "From: sender@example.com\r\n" +
            "To: receiver@example.com\r\n" +
            "Subject: Graph send test\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
            "hello from Graph\r\n"
        return MimeMessage.parseMimeMessage(ByteArrayInputStream(rawMime.toByteArray(UTF_8)), false)
    }

    private fun createClient(
        tokenProvider: GraphApiTestTokenProvider = GraphApiTestTokenProvider(),
        logger: TestLogger = TestLogger(),
    ): GraphApiClient {
        return GraphApiClient(
            okHttpClient = OkHttpClient(),
            tokenProvider = tokenProvider,
            logger = logger,
            baseUrl = server.url("v1.0/"),
        )
    }

    private fun pageWithDeltaLink(deltaLink: String): String = """
        {
          "value": [],
          "@odata.deltaLink": "$deltaLink"
        }
    """.trimIndent()
}

private class GraphApiTestTokenProvider : OAuth2TokenProvider {
    private var tokenCount = 0
    var invalidations = 0
        private set

    override val usernames: Set<String> = emptySet()

    override fun getToken(timeoutMillis: Long): String {
        tokenCount++
        return "token-$tokenCount"
    }

    override fun invalidateToken() {
        invalidations++
    }
}
