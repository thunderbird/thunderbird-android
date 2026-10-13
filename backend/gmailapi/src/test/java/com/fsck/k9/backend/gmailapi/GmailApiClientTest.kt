package com.fsck.k9.backend.gmailapi

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class GmailApiClientTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list messages includes Gmail q label pagination and spam trash parameters`() {
        server.enqueue(MockResponse().setBody("""{"messages":[{"id":"message-1"}],"nextPageToken":"next"}"""))
        server.start()
        val client = createClient()

        val page = client.listMessages(
            labelId = "SPAM",
            pageToken = "previous",
            maxResults = 500,
            query = "{subject:\"hello\" from:\"hello\"}",
        )

        assertEquals("message-1", page.messages.single().id)
        assertEquals("next", page.nextPageToken)
        val request = server.takeRequest()
        assertEquals("SPAM", request.requestUrl!!.queryParameter("labelIds"))
        assertEquals("true", request.requestUrl!!.queryParameter("includeSpamTrash"))
        assertEquals("500", request.requestUrl!!.queryParameter("maxResults"))
        assertEquals("previous", request.requestUrl!!.queryParameter("pageToken"))
        assertEquals("{subject:\"hello\" from:\"hello\"}", request.requestUrl!!.queryParameter("q"))
        assertEquals("Bearer access-token", request.getHeader("Authorization"))
    }

    @Test
    fun `history separates new messages from label changes and deletions`() {
        server.enqueue(
            MockResponse().setBody(
                """{"history":[{"id":"20","messagesAdded":[{"message":{"id":"new-message"}}],"labelsAdded":[{"message":{"id":"older-labelled-message"},"labelIds":["INBOX"]}],"messagesDeleted":[{"message":{"id":"deleted-message"}}],"labelsRemoved":[{"message":{"id":"moved-message"},"labelIds":["INBOX"]}]}],"historyId":"cursor-2"}""",
            ),
        )
        server.start()

        val changes = createClient().history("cursor-1")

        assertEquals(listOf("new-message"), changes.addedMessageIds)
        assertEquals(setOf("new-message", "older-labelled-message", "deleted-message", "moved-message"), changes.changedMessageIds)
        assertEquals(setOf("deleted-message"), changes.deletedMessageIds)
        assertEquals("cursor-2", changes.latestHistoryId)
        assertEquals("cursor-1", server.takeRequest().requestUrl!!.queryParameter("startHistoryId"))
    }

    @Test
    fun `403 response exposes only the safe Gmail API reason code`() {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """{"error":{"errors":[{"reason":"accessNotConfigured","message":"private response details"}]}}""",
            ),
        )
        server.start()

        val error = assertThrows(GmailApiException::class.java) { createClient().labels() }

        assertEquals(403, error.statusCode)
        assertEquals("accessNotConfigured", error.reason)
        assertEquals("Gmail REST request failed (403, reason=accessNotConfigured)", error.message)
        assertFalse(error.message!!.contains("private response details"))
    }

    @Test
    fun `401 invalidates the expired token and retries once with a fresh token`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"labels":[]}"""))
        server.start()
        val tokens = RefreshingTokenProvider()

        assertTrue(createClient(tokens).labels().isEmpty())

        assertEquals(1, tokens.invalidations)
        assertEquals("Bearer expired-token", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `repeated 401 becomes an authentication failure after one token refresh`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        val tokens = RefreshingTokenProvider()

        assertThrows(AuthenticationFailedException::class.java) { createClient(tokens).labels() }

        assertEquals(1, tokens.invalidations)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `safe GET retries transport and transient service failures`() {
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody("""{"labels":[]}"""))
        server.start()
        var transportAttempts = 0
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                if (transportAttempts++ == 0) throw IOException("temporary network loss")
                chain.proceed(chain.request())
            }
            .build()

        assertTrue(createClient(httpClient = client).labels().isEmpty())

        assertEquals(3, transportAttempts)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `transient service failure does not replay a mutating request`() {
        server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0").setBody("{}"))
        server.start()

        assertThrows(GmailApiException::class.java) { createClient().modify("message-1", add = listOf("STARRED")) }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `send transport failure becomes a permanent outcome-unknown error`() {
        server.start()
        val unavailableNetwork = OkHttpClient.Builder()
            .addInterceptor { throw IOException("network unavailable") }
            .build()

        val error = assertThrows(GmailSendOutcomeUnknownException::class.java) {
            createClient(httpClient = unavailableNetwork).send(MimeMessage())
        }

        assertTrue(error.isPermanentFailure)
    }

    @Test
    fun `All Mail uses a virtual Gmail query instead of an unsupported label ID`() {
        server.enqueue(MockResponse().setBody("""{"messages":[]}"""))
        server.start()
        createClient().listMessages("ALL_MAIL", maxResults = 10, query = "subject:hello")

        val requestUrl = server.takeRequest().requestUrl!!
        assertNull(requestUrl.queryParameter("labelIds"))
        assertEquals("-in:trash -in:spam subject:hello", requestUrl.queryParameter("q"))
        assertNull(requestUrl.queryParameter("includeSpamTrash"))
    }

    @Test
    fun `metadata fetch retains labels and size for partial message policy`() {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"message-1","threadId":"thread-1","labelIds":["INBOX","UNREAD"],"sizeEstimate":12345,"internalDate":"1700000000000","payload":{"mimeType":"text/plain","headers":[{"name":"Subject","value":"Hello"}]}}""",
            ),
        )
        server.start()

        val message = createClient().getMessage("message-1", "metadata")

        assertEquals(12345L, message.sizeEstimate)
        assertEquals(1700000000000L, message.internalDateMillis)
        assertEquals(listOf("INBOX", "UNREAD"), message.labelIds)
        assertEquals("metadata", server.takeRequest().requestUrl!!.queryParameter("format"))
    }

    @Test
    fun `copy uses the Gmail native copy endpoint`() {
        server.enqueue(MockResponse().setBody("""{"id":"copy-1"}"""))
        server.start()

        assertEquals("copy-1", createClient().copy("source-1", "Label_1"))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/gmail/v1/users/me/messages/source-1/copy", request.path)
        assertEquals("[\"Label_1\"]", request.body.readUtf8().substringAfter("\"labelIds\":").substringBefore('}'))
    }

    @Test
    fun `permanent deletion uses the Gmail messages delete endpoint`() {
        server.enqueue(MockResponse().setResponseCode(204))
        server.start()

        createClient().deleteForever("message-1")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/gmail/v1/users/me/messages/message-1", request.path)
    }

    @Test
    fun `recursive MIME keeps multipart structure and defers attachment ID bytes`() {
        server.start()
        val payload: Map<String, Any?> = mapOf(
            "mimeType" to "multipart/mixed",
            "headers" to listOf(mapOf("name" to "Content-Type", "value" to "multipart/mixed; boundary=\"root\"")),
            "parts" to listOf(
                mapOf(
                    "mimeType" to "multipart/alternative",
                    "headers" to listOf(mapOf("name" to "Content-Type", "value" to "multipart/alternative; boundary=\"alt\"")),
                    "parts" to listOf(
                        mapOf("mimeType" to "text/plain", "body" to mapOf("data" to "cGxhaW4=")),
                        mapOf("mimeType" to "text/html", "body" to mapOf("data" to "PHA+aHRtbDwvcD4=")),
                    ),
                ),
                mapOf(
                    "mimeType" to "application/octet-stream",
                    "headers" to listOf(mapOf("name" to "Content-Disposition", "value" to "attachment; filename=sample.bin")),
                    "body" to mapOf("attachmentId" to "attachment-1", "data" to "dGVzdA=="),
                ),
            ),
        )

        val mime = String(createClient().createMime(GmailMessage("message-1", null, emptyList(), payload)))

        assertTrue(mime.contains("--root"))
        assertTrue(mime.contains("--alt"))
        assertTrue(mime.contains("X-Thunderbird-Gmail-Attachment-Id:"))
        assertFalse(mime.contains("dGVzdA=="))
    }

    @Test
    fun `query builder matches full text or header search and supported flags`() {
        assertEquals(
            "\"budget review\" is:read -is:starred",
            GmailSearchQuery.build("budget review", setOf(Flag.SEEN), setOf(Flag.FLAGGED), true),
        )
        assertEquals(
            "{subject:\"budget review\" from:\"budget review\" to:\"budget review\" cc:\"budget review\" bcc:\"budget review\"}",
            GmailSearchQuery.build("budget review", null, null, false),
        )
    }

    @Test
    fun `query builder omits blank query and rejects unsupported flags`() {
        assertNull(GmailSearchQuery.build("  ", null, null, true))
        assertThrows(UnsupportedOperationException::class.java) {
            GmailSearchQuery.build(null, setOf(Flag.ANSWERED), null, true)
        }
    }

    private fun createClient(
        tokenProvider: OAuth2TokenProvider = StaticTokenProvider,
        httpClient: OkHttpClient = OkHttpClient(),
    ): GmailApiClient {
        val baseUrl = server.url("gmail/v1/users")
        return GmailApiClient(httpClient, tokenProvider, TestLogger(), baseUrl)
    }
}

private object StaticTokenProvider : OAuth2TokenProvider {
    override val usernames = emptySet<String>()
    override fun getToken(timeoutMillis: Long): String = "access-token"
    override fun invalidateToken() = Unit
}

private class RefreshingTokenProvider : OAuth2TokenProvider {
    override val usernames = emptySet<String>()
    var invalidations = 0
        private set

    override fun getToken(timeoutMillis: Long): String = if (invalidations == 0) "expired-token" else "fresh-token"

    override fun invalidateToken() {
        invalidations++
    }
}
