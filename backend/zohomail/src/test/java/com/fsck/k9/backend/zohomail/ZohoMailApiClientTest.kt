package com.fsck.k9.backend.zohomail

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.squareup.moshi.Moshi
import net.thunderbird.core.logging.testing.TestLogger
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ZohoMailApiClientTest {
    @Test
    fun `origin must be an official persisted Zoho origin`() {
        assertThrows(IllegalArgumentException::class.java) {
            ZohoMailApiClient.requireOfficialOrigin("https://mail.example.invalid")
        }
        assertEquals("https://mail.zohocloud.ca", ZohoMailApiClient.requireOfficialOrigin("https://mail.zohocloud.ca/"))
    }

    @Test
    fun `401 invalidates the expired token and retries once with a fresh token`() {
        val requests = mutableListOf<Request>()
        val tokens = RefreshingTokenProvider()
        val client = createClient(
            responses = listOf(
                FakeResponse(401),
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(200, """{"data":{"folders":[]}}"""),
            ),
            requests = requests,
            tokenProvider = tokens,
        )

        client.listFolders()

        assertEquals(1, tokens.invalidations)
        assertEquals("Zoho-oauthtoken expired-token", requests[0].header("Authorization"))
        assertEquals("Zoho-oauthtoken fresh-token", requests[1].header("Authorization"))
        assertEquals(3, requests.size)
    }

    @Test
    fun `repeated 401 becomes an authentication failure after one token refresh`() {
        val requests = mutableListOf<Request>()
        val tokens = RefreshingTokenProvider()
        val client = createClient(
            responses = listOf(FakeResponse(401), FakeResponse(401)),
            requests = requests,
            tokenProvider = tokens,
        )

        assertThrows(AuthenticationFailedException::class.java) { client.listFolders() }

        assertEquals(1, tokens.invalidations)
        assertEquals(2, requests.size)
    }

    @Test
    fun `list messages reads documented receivedtime and advances the one-based offset`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(200, """{"data":[{"messageId":456,"receivedtime":1710915486000,"subject":"Message"}]}"""),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        val page = client.listMessages("789", start = 21, limit = 1)

        assertEquals(1710915486000L, page.messages.single().receivedTime)
        assertEquals(22, page.nextStart)
        assertEquals("21", requests.last().url.queryParameter("start"))
        assertEquals("false", requests.last().url.queryParameter("sortorder"))
    }

    @Test
    fun `list messages includes recipients and parses the separate sender display name`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(
                    200,
                    """
                        {"data":[{"messageId":"456","fromAddress":"sender@example.test",
                        "sender":"Sender Name","toAddress":"&quot;Recipient Name&quot;&lt;recipient@example.test&gt;"}]}
                    """.trimIndent(),
                ),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        val summary = client.listMessages("789", limit = 1).messages.single()

        assertEquals("true", requests.last().url.queryParameter("includeto"))
        assertEquals("sender@example.test", summary.from)
        assertEquals("Sender Name", summary.sender)
        assertEquals("&quot;Recipient Name&quot;&lt;recipient@example.test&gt;", summary.to)
    }

    @Test
    fun `attachment info preserves regular and inline size and MIME metadata`() {
        val requests = mutableListOf<Request>()
        val testSubject = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(
                    200,
                    """{
                        "data": {
                            "attachments": [
                                {"attachmentId":"55", "attachmentName":"report.pdf", "contentType":"application/pdf", "size":1234}
                            ],
                            "inline": [
                                {"attachmentId":"56", "fileName":"diagram.png", "mimeType":"image/png", "attachmentSize":42, "contentId":"image-1"}
                            ]
                        }
                    }""",
                ),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        val attachments = testSubject.attachmentInfo("789", "456")

        assertEquals(2, attachments.size)
        assertEquals("application/pdf", attachments[0].contentType)
        assertEquals(1234L, attachments[0].size)
        assertEquals("image/png", attachments[1].contentType)
        assertEquals(42L, attachments[1].size)
        assertTrue(attachments[1].inline)
        assertEquals("/api/accounts/123/folders/789/messages/456/attachmentinfo", requests.last().url.encodedPath)
        assertEquals("true", requests.last().url.queryParameter("includeInline"))
    }

    @Test
    fun `full message parses the separate Zoho sender display name`() {
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(
                    200,
                    """
                        {"data":{"messageId":"456","content":"body","fromAddress":"sender@example.test",
                        "sender":"Sender Name","toAddress":"recipient@example.test","sentDateInGMT":1710915486000}}
                    """.trimIndent(),
                ),
            ),
            requests = mutableListOf(),
            tokenProvider = StaticTokenProvider,
        )

        val testSubject = client.getMessage("789", "456")

        assertEquals("sender@example.test", testSubject.from)
        assertEquals("Sender Name", testSubject.sender)
        assertEquals("recipient@example.test", testSubject.to)
    }

    @Test
    fun `message date is read from the documented raw headers endpoint`() {
        val rawHeaders = listOf(
            "From: sender@example.invalid",
            "dAte:",
            "\tWed, 20 Mar 2024 11:48:06 +0530",
            "Subject: Hello",
            "",
        ).joinToString("\r\n")
        val headerContentJson = Moshi.Builder().build().adapter(String::class.java).toJson(rawHeaders)
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(200, """{"data":{"messageId":456,"content":"body"}}"""),
                FakeResponse(200, """{"data":{"headerContent":$headerContentJson}}"""),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        val message = client.getMessage("789", "456")

        assertEquals("Wed, 20 Mar 2024 11:48:06 +0530", message.dateHeader)
        assertEquals("/api/accounts/123/folders/789/messages/456/header", requests.last().url.encodedPath)
        assertEquals("true", requests.last().url.queryParameter("raw"))
    }

    @Test
    fun `safe GET retries a transient service failure honoring Retry-After`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(503, headers = mapOf("Retry-After" to "0")),
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(200, """{"data":{"folders":[]}}"""),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        client.listFolders()

        assertEquals(3, requests.size)
        assertEquals("GET", requests[0].method)
        assertEquals("GET", requests[1].method)
    }

    @Test
    fun `transient service failure does not replay a mutating request`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(503, headers = mapOf("Retry-After" to "0")),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        assertThrows(ZohoMailApiException::class.java) { client.setRead("1", "2", true) }

        assertEquals(2, requests.size)
        assertEquals("PUT", requests.last().method)
    }

    @Test
    fun `upload sends a 3 MiB attachment as raw binary with documented query parameters`() {
        val requests = mutableListOf<Request>()
        val bytes = ByteArray(3 * 1024 * 1024) { (it % 251).toByte() }
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(
                    200,
                    """
                        {"status":{"code":200,"description":"success"},"data":{"storeName":"store-1",
                        "attachmentPath":"/Mail/report.pdf","attachmentName":"report.pdf"}}
                    """.trimIndent(),
                ),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )

        val uploaded = client.uploadAttachment("report.pdf", bytes, inline = false)

        val request = requests.last()
        assertEquals("report.pdf", uploaded.name)
        assertEquals("/Mail/report.pdf", uploaded.path)
        assertEquals("store-1", uploaded.storeName)
        assertEquals("POST", request.method)
        assertEquals("/api/accounts/123/messages/attachments", request.url.encodedPath)
        assertEquals("report.pdf", request.url.queryParameter("fileName"))
        assertEquals("false", request.url.queryParameter("isInline"))
        assertEquals("application/octet-stream", request.header("Content-Type"))
        assertEquals(bytes.size.toLong(), request.body?.contentLength())
        val uploadedBytes = Buffer().also { request.body?.writeTo(it) }.readByteArray()
        assertArrayEquals(bytes, uploadedBytes)
    }

    @Test
    fun `send uses the documented attachment reference field names`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
                FakeResponse(200, """{"status":{"code":200,"description":"success"}}"""),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
        )
        val attachment = mapOf(
            "attachmentName" to "report.pdf",
            "attachmentPath" to "/Mail/report.pdf",
            "storeName" to "store-1",
        )

        client.send(
            mapOf(
                "fromAddress" to "user@example.invalid",
                "toAddress" to "recipient@example.invalid",
                "attachments" to listOf(attachment),
            ),
        )

        val request = requests.last()
        val jsonBody = Buffer().also { request.body?.writeTo(it) }.readUtf8()
        val body = Moshi.Builder().build().adapter(Map::class.java).fromJson(jsonBody) as Map<*, *>
        val sentAttachment = (body["attachments"] as List<*>).single() as Map<*, *>
        assertEquals("POST", request.method)
        assertEquals("/api/accounts/123/messages", request.url.encodedPath)
        assertEquals("application/json; charset=utf-8", request.header("Content-Type"))
        assertEquals("report.pdf", sentAttachment["attachmentName"])
        assertEquals("/Mail/report.pdf", sentAttachment["attachmentPath"])
        assertEquals("store-1", sentAttachment["storeName"])
    }

    @Test
    fun `send transport failure becomes a permanent outcome-unknown error`() {
        val requests = mutableListOf<Request>()
        val client = createClient(
            responses = listOf(
                FakeResponse(200, """{"data":[{"accountId":"123","emailAddress":["user@example.invalid"]}]}"""),
            ),
            requests = requests,
            tokenProvider = StaticTokenProvider,
            failRequestNumber = 2,
        )

        val error = assertThrows(ZohoSendOutcomeUnknownException::class.java) {
            client.send(mapOf("subject" to "test"))
        }

        assertTrue(error.isPermanentFailure)
        assertEquals(2, requests.size)
    }

    private fun createClient(
        responses: List<FakeResponse>,
        requests: MutableList<Request>,
        tokenProvider: OAuth2TokenProvider,
        failRequestNumber: Int? = null,
    ): ZohoMailApiClient {
        val queuedResponses = responses.toMutableList()
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requests += request
                if (requests.size == failRequestNumber) throw IOException("network unavailable")
                val scripted = queuedResponses.removeAt(0)
                val builder = Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(scripted.statusCode)
                    .message("scripted response")
                    .body(scripted.body.toResponseBody("application/json".toMediaType()))
                scripted.headers.forEach { (name, value) -> builder.addHeader(name, value) }
                builder.build()
            }
            .build()
        return ZohoMailApiClient(
            client = httpClient,
            tokenProvider = tokenProvider,
            logger = TestLogger(),
            origin = "https://mail.zoho.com",
            mailboxAddress = "user@example.invalid",
        )
    }

    private data class FakeResponse(
        val statusCode: Int,
        val body: String = "{}",
        val headers: Map<String, String> = emptyMap(),
    )
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
