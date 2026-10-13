package com.fsck.k9.backend.zohomail

import com.fsck.k9.backend.api.TransientApiRequestRetry
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Native Zoho Mail REST client. Its origin must come from verified Zoho OAuth callback data. */
internal class ZohoMailApiClient(
    client: OkHttpClient,
    private val tokenProvider: OAuth2TokenProvider,
    private val logger: Logger,
    origin: String,
    private val mailboxAddress: String,
) {
    private val origin = requireOfficialOrigin(origin)
    private val originUrl = this.origin.toHttpUrl()
    private val client = client.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val retryAfter = response.header("Retry-After")
            if (
                response.code == HTTP_SERVICE_UNAVAILABLE &&
                retryAfter?.trim()?.toLongOrNull() == 0L
            ) {
                response.newBuilder()
                    .header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER, checkNotNull(retryAfter))
                    .removeHeader("Retry-After")
                    .build()
            } else {
                response
            }
        }
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val moshi = Moshi.Builder().build()
    private val mapAdapter: JsonAdapter<Map<*, *>> = moshi.adapter(Map::class.java)

    @Volatile
    private var cachedAccountId: String? = null

    fun listFolders(): List<ZohoFolder> {
        val root = requestJson("GET", url("api", "accounts", accountId(), "folders"))
        return root.dataList("folders").mapNotNull { it.toFolder() }
    }

    /** Zoho's list API is 1-based and has no next-page token. */
    fun listMessages(folderId: String, start: Int = 1, limit: Int = MAX_PAGE_SIZE): ZohoPage {
        require(start >= 1)
        require(limit in 1..MAX_PAGE_SIZE)
        val target = url("api", "accounts", accountId(), "messages", "view") {
            addQueryParameter("folderId", remoteId(folderId).toString())
            addQueryParameter("start", start.toString())
            addQueryParameter("limit", limit.toString())
            addQueryParameter("sortBy", "date")
            addQueryParameter("sortorder", "false")
            addQueryParameter("includeto", "true")
        }
        val root = requestJson("GET", target)
        return ZohoPage(root.dataList("messages").mapNotNull { it.toSummary() }, start, limit)
    }

    fun searchMessages(searchKey: String, start: Int = 1, limit: Int = MAX_PAGE_SIZE): ZohoPage {
        require(start >= 1)
        require(limit in 1..MAX_PAGE_SIZE)
        val target = url("api", "accounts", accountId(), "messages", "search") {
            addQueryParameter("searchKey", searchKey)
            addQueryParameter("receivedTime", System.currentTimeMillis().toString())
            addQueryParameter("start", start.toString())
            addQueryParameter("limit", limit.toString())
            addQueryParameter("includeto", "true")
        }
        val root = requestJson("GET", target)
        return ZohoPage(root.dataList("messages").mapNotNull { it.toSummary() }, start, limit)
    }

    fun getMessage(folderId: String, messageId: String): ZohoMessage {
        val root = requestJson(
            "GET",
            url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString(), "content") {
                addQueryParameter("includeBlockContent", "true")
            },
        )
        val data = root.dataMap()
        val content = data.string("content") ?: throw IOException("Zoho Mail message response contained no content")
        val sentTime = data.long("sentDateInGMT") ?: data.long("sentDate")
        val dateHeader = if (sentTime == null) {
            runCatching { messageHeaders(folderId, messageId) }
                .onFailure { logger.warn(TAG) { "Zoho Mail could not retrieve message Date header" } }
                .getOrNull()
                .headerValue("Date")
        } else {
            null
        }
        return ZohoMessage(
            id = messageId,
            subject = data.string("subject"),
            content = content,
            from = data.string("fromAddress"),
            to = data.string("toAddress"),
            sentTime = sentTime,
            messageIdHeader = data.string("messageIdHeader") ?: data.string("internetMessageId"),
            dateHeader = dateHeader,
            sender = data.string("sender"),
        )
    }

    fun messageHeaders(folderId: String, messageId: String): String? {
        val root = requestJson(
            "GET",
            url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString(), "header") {
                addQueryParameter("raw", "true")
            },
        )
        return root.dataMap().string("headerContent")
    }

    fun attachmentInfo(folderId: String, messageId: String): List<ZohoAttachment> {
        val root = requestJson(
            "GET",
            url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString(), "attachmentinfo") {
                addQueryParameter("includeInline", "true")
            },
        )
        val data = root.dataMap()
        val ordinary = data.mapList("attachments").mapNotNull { it.toAttachment(inline = false) }
        val inline = data.mapList("inline").mapNotNull { it.toAttachment(inline = true) }
        return ordinary + inline
    }

    fun attachmentBytes(folderId: String, messageId: String, attachmentId: String): ByteArray = requestBytes(
        "GET",
        url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString(), "attachments", remoteId(attachmentId).toString()),
    )

    fun inlineBytes(folderId: String, messageId: String, contentId: String, fileName: String?): ByteArray = requestBytes(
        "GET",
        url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString(), "inline") {
            addQueryParameter("contentId", contentId.removePrefix("<").removeSuffix(">"))
            fileName?.takeIf(String::isNotBlank)?.let { addQueryParameter("fileName", it) }
        },
    )

    /** Full RFC MIME is fetched only from Backend.downloadCompleteMessage(). */
    fun originalMessage(messageId: String): String {
        val root = requestJson("GET", url("api", "accounts", accountId(), "messages", remoteId(messageId).toString(), "originalmessage"))
        return root.dataMap().string("content") ?: throw IOException("Zoho original-message response contained no MIME content")
    }

    fun setRead(folderId: String, messageId: String, value: Boolean) {
        updateMessage(
            mode = if (value) "markAsRead" else "markAsUnread",
            folderId = folderId,
            messageIds = listOf(messageId),
        )
    }

    fun setFlagged(folderId: String, messageId: String, value: Boolean) {
        updateMessage(
            mode = "setFlag",
            folderId = folderId,
            messageIds = listOf(messageId),
            extra = mapOf("flagid" to if (value) "important" else "flag_not_set"),
        )
    }

    fun move(folderId: String, targetFolderId: String, messageId: String) {
        updateMessage(
            mode = "moveMessage",
            folderId = folderId,
            messageIds = listOf(messageId),
            extra = mapOf("destfolderId" to remoteId(targetFolderId)),
        )
    }

    fun delete(folderId: String, messageId: String, permanently: Boolean) {
        val target = url("api", "accounts", accountId(), "folders", remoteId(folderId).toString(), "messages", remoteId(messageId).toString()) {
            addQueryParameter("expunge", permanently.toString())
        }
        requestJson("DELETE", target)
    }

    fun emptyFolder(folderId: String) {
        val body = mapOf("mode" to "emptyFolder")
        requestJson("PUT", url("api", "accounts", accountId(), "folders", remoteId(folderId).toString()), jsonBody(body))
    }

    fun uploadAttachment(fileName: String, bytes: ByteArray, inline: Boolean): ZohoUploadedAttachment {
        require(bytes.isNotEmpty()) { "Zoho Mail cannot upload an empty attachment" }
        require(bytes.size <= MAX_ATTACHMENT_BYTES) { "Attachment exceeds the Zoho Mail REST size limit" }
        val target = url("api", "accounts", accountId(), "messages", "attachments") {
            addQueryParameter("fileName", sanitizeFileName(fileName))
            addQueryParameter("isInline", inline.toString())
        }
        val response = requestJson(
            "POST",
            target,
            bytes.toRequestBody(BINARY_CONTENT_TYPE.toMediaType()),
            requestContentType = BINARY_CONTENT_TYPE,
        )
        val data = response.dataMap()
        val store = data.string("storeName") ?: throw IOException("Zoho attachment upload returned no storeName")
        val path = data.string("attachmentPath") ?: throw IOException("Zoho attachment upload returned no attachmentPath")
        val name = data.string("attachmentName") ?: throw IOException("Zoho attachment upload returned no attachmentName")
        return ZohoUploadedAttachment(name, path, store)
    }

    fun send(payload: Map<String, Any?>) {
        val target = url("api", "accounts", accountId(), "messages")
        executeSendRequest { requestJson("POST", target, jsonBody(payload)) }
    }

    fun reply(messageId: String, payload: Map<String, Any?>) {
        val target = url("api", "accounts", accountId(), "messages", remoteId(messageId).toString())
        executeSendRequest { requestJson("POST", target, jsonBody(payload)) }
    }

    private inline fun executeSendRequest(sendRequest: () -> Unit) {
        try {
            sendRequest()
        } catch (error: AuthenticationFailedException) {
            throw error
        } catch (error: IOException) {
            throw error.toSendFailure()
        }
    }

    private fun IOException.toSendFailure(): Exception = when (this) {
        is ZohoMailApiException -> {
            if (statusCode == HTTP_REQUEST_TIMEOUT || statusCode >= HTTP_INTERNAL_SERVER_ERROR) {
                ZohoSendOutcomeUnknownException(this)
            } else {
                this
            }
        }

        else -> ZohoSendOutcomeUnknownException(this)
    }

    private fun updateMessage(
        mode: String,
        folderId: String,
        messageIds: List<String>,
        extra: Map<String, Any?> = emptyMap(),
    ) {
        val body = buildMap<String, Any?> {
            put("mode", mode)
            put("messageId", messageIds.map(::remoteId))
            put("isFolderSpecific", true)
            put("folderId", remoteId(folderId))
            putAll(extra)
        }
        requestJson("PUT", url("api", "accounts", accountId(), "updatemessage"), jsonBody(body))
    }

    private fun accountId(): String {
        cachedAccountId?.let { return it }
        synchronized(this) {
            cachedAccountId?.let { return it }
            val accounts = requestJson("GET", url("api", "accounts")).dataList("accounts")
            val expected = mailboxAddress.trim().lowercase(Locale.ROOT)
            val match = accounts.firstOrNull { account ->
                account.list("emailAddress").any { it.lowercase(Locale.ROOT) == expected } ||
                    listOf("primaryEmailAddress", "mailboxAddress", "incomingUserName", "emailAddress")
                        .mapNotNull { key -> account.string(key) }
                        .any { it.trim().lowercase(Locale.ROOT) == expected }
            } ?: throw IOException("Zoho Mail account discovery did not find the authenticated mailbox")
            val value = match.string("accountId") ?: match.string("id")
                ?: throw IOException("Zoho Mail account discovery returned no account ID")
            val normalized = normalizeNumericId(value)
                ?: throw IOException("Zoho Mail account discovery returned an invalid account ID")
            cachedAccountId = normalized
            return normalized
        }
    }

    private fun requestJson(
        method: String,
        url: HttpUrl,
        body: RequestBody? = null,
        requestContentType: String = "application/json; charset=utf-8",
    ): Map<String, Any?> {
        val responseText = execute(method, url, body, requestContentType, accept = "application/json")
        if (responseText.isBlank()) return emptyMap()
        val parsed = mapAdapter.fromJson(responseText).orEmpty().entries.associate { it.key.toString() to it.value }
        val status = parsed["status"] as? Map<*, *>
        val code = status?.get("code")?.toString()?.toIntOrNull()
        if (code != null && code !in 200..299) {
            logger.warn(TAG) { "Zoho Mail returned status=$code path=${url.encodedPath}" }
            throw ZohoMailApiException(code, "Zoho Mail request failed with API status $code")
        }
        return parsed
    }

    private fun requestBytes(method: String, url: HttpUrl): ByteArray {
        val response = executeResponse(method, url, null, null, accept = "application/octet-stream")
        response.use {
            val body = it.body?.bytes() ?: ByteArray(0)
            if (!it.isSuccessful) {
                logger.warn(TAG) { "Zoho Mail binary request failed status=${it.code} path=${url.encodedPath}" }
                throw ZohoMailApiException(it.code, "Zoho Mail attachment request failed with HTTP ${it.code}")
            }
            return body
        }
    }

    private fun execute(
        method: String,
        url: HttpUrl,
        body: RequestBody?,
        contentType: String?,
        accept: String,
    ): String {
        val response = executeResponse(method, url, body, contentType, accept)
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                logger.warn(TAG) { "Zoho Mail request failed status=${it.code} path=${url.encodedPath}" }
                throw ZohoMailApiException(it.code, "Zoho Mail request failed with HTTP ${it.code}")
            }
            return text
        }
    }

    private fun executeResponse(
        method: String,
        url: HttpUrl,
        body: RequestBody?,
        contentType: String?,
        accept: String,
    ): okhttp3.Response {
        var authorizationAttempt = 1
        while (authorizationAttempt <= MAX_AUTH_ATTEMPTS) {
            val token = tokenProvider.getToken(OAuth2TokenProvider.OAUTH2_TIMEOUT.toLong())
            val builder = Request.Builder()
                .url(url)
                .header("Authorization", "Zoho-oauthtoken $token")
                .header("Accept", accept)
            if (contentType != null) builder.header("Content-Type", contentType)
            val request = builder.method(method, body).build()
            val response = executeWithTransientRetry(request)
            if (response.code == HTTP_UNAUTHORIZED) {
                response.close()
                if (authorizationAttempt < MAX_AUTH_ATTEMPTS) {
                    tokenProvider.invalidateToken()
                    authorizationAttempt++
                    continue
                }
                throw AuthenticationFailedException("Zoho Mail rejected the access token")
            }
            return response
        }
        throw AuthenticationFailedException("Zoho Mail authentication failed")
    }

    private fun executeWithTransientRetry(request: Request): okhttp3.Response {
        for (attempt in 1..TransientApiRequestRetry.MAX_ATTEMPTS) {
            val response = try {
                client.newCall(request).execute()
            } catch (error: IOException) {
                if (!TransientApiRequestRetry.shouldRetryTransport(request.method, attempt)) {
                    throw IOException("Zoho Mail REST request failed", error)
                }
                TransientApiRequestRetry.waitBeforeRetry(attempt)
                null
            }
            if (
                response != null &&
                TransientApiRequestRetry.shouldRetryResponse(request.method, response.code, attempt)
            ) {
                val retryAfter = response.header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER)
                    ?: response.header("Retry-After")
                response.close()
                TransientApiRequestRetry.waitBeforeRetry(attempt, retryAfter)
            } else if (response != null) {
                return response
            }
        }
        throw IOException("Zoho Mail REST request retry limit reached")
    }

    private fun jsonBody(value: Map<String, Any?>): RequestBody = mapAdapter.toJson(value)
        .toRequestBody("application/json; charset=utf-8".toMediaType())

    private fun url(vararg pathSegments: String, query: HttpUrl.Builder.() -> Unit = {}): HttpUrl =
        originUrl.newBuilder().apply {
            pathSegments.forEach(::addPathSegment)
            query()
        }.build()

    private fun Map<String, Any?>.dataMap(): Map<String, Any?> =
        (this["data"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } ?: this

    private fun Map<String, Any?>.dataList(nestedKey: String): List<Map<String, Any?>> {
        val data = this["data"]
        val rows = data as? List<*> ?: ((data as? Map<*, *>)?.get(nestedKey) as? List<*>) ?: (this[nestedKey] as? List<*>) ?: emptyList<Any?>()
        return rows.mapNotNull { row ->
            (row as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }
        }
    }

    private fun Map<String, Any?>.list(key: String): List<String> = (this[key] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()

    private fun Map<String, Any?>.mapList(key: String): List<Map<String, Any?>> =
        (this[key] as? List<*>)?.mapNotNull { row ->
            (row as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }
        }.orEmpty()

    private fun Map<String, Any?>.string(key: String): String? = this[key]
        ?.takeUnless { it == "null" }
        ?.let { value ->
            when (value) {
                is Number -> value.toLong().toString()
                else -> value.toString()
            }
        }
        ?.takeIf(String::isNotBlank)

    private fun Map<String, Any?>.long(key: String): Long? = when (val value = this[key]) {
        is Number -> value.toLong()
        else -> value?.toString()?.toLongOrNull()
    }

    private fun String?.headerValue(name: String): String? = this
        ?.replace(Regex("\\r?\\n[ \\t]+"), " ")
        ?.lineSequence()
        ?.filter(String::isNotBlank)
        ?.firstNotNullOfOrNull { line ->
            val separator = line.indexOf(':')
            if (separator >= 0 && line.substring(0, separator).equals(name, ignoreCase = true)) {
                line.substring(separator + 1).trim().takeIf(String::isNotBlank)
            } else {
                null
            }
        }

    private fun Map<String, Any?>.toFolder(): ZohoFolder? {
        val id = string("folderId") ?: string("folder_id") ?: return null
        val name = string("folderName") ?: string("name") ?: return null
        return ZohoFolder(id, name, string("folderType") ?: string("type"), string("path"))
    }

    private fun Map<String, Any?>.toSummary(): ZohoSummary? {
        val id = string("messageId") ?: string("message_id") ?: string("msgId") ?: return null
        val status = string("status")?.lowercase(Locale.ROOT)
        val read = when (status) {
            "read", "1", "true" -> true
            "unread", "0", "false" -> false
            else -> this["isRead"] as? Boolean ?: false
        }
        val flag = string("flagid") ?: string("flagId")
        val flagged = (flag?.toLongOrNull()?.let { it > 0 } ?: false) || (this["isFlagged"] as? Boolean == true)
        return ZohoSummary(
            id = id,
            receivedTime = long("receivedtime") ?: long("receivedTime") ?: long("receivedDate") ?: long("date"),
            subject = string("subject"),
            read = read,
            flagged = flagged,
            threadId = string("threadId") ?: string("thread_id"),
            size = long("size") ?: long("messageSize"),
            from = string("fromAddress"),
            folderId = string("folderId"),
            sender = string("sender"),
            to = string("toAddress"),
        )
    }

    private fun Map<String, Any?>.toAttachment(inline: Boolean): ZohoAttachment? {
        val id = string("attachmentId") ?: string("id") ?: return null
        return ZohoAttachment(
            id = id,
            name = string("attachmentName") ?: string("fileName") ?: string("name"),
            contentType = string("contentType") ?: string("mimeType"),
            size = long("attachmentSize") ?: long("size"),
            inline = inline,
            contentId = string("cid") ?: string("contentId"),
        )
    }

    private fun remoteId(value: String): Long = value.toLongOrNull()?.takeIf { it >= 0 }
        ?: throw IllegalArgumentException("Invalid Zoho Mail identifier")

    private fun normalizeNumericId(value: String): String? {
        val number = value.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        return number.toString()
    }

    private fun sanitizeFileName(value: String): String = value
        .replace('/', '_')
        .replace('\\', '_')
        .replace('\r', '_')
        .replace('\n', '_')
        .ifBlank { "attachment" }

    companion object {
        private const val TAG = "ZohoMailApi"
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 45L
        private const val WRITE_TIMEOUT_SECONDS = 60L
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_INTERNAL_SERVER_ERROR = 500
        private const val MAX_AUTH_ATTEMPTS = 2
        private const val MAX_PAGE_SIZE = 200
        private const val MAX_ATTACHMENT_BYTES = 30 * 1024 * 1024
        private const val BINARY_CONTENT_TYPE = "application/octet-stream"
        val OFFICIAL_ORIGINS = setOf(
            "https://mail.zoho.com",
            "https://mail.zoho.eu",
            "https://mail.zoho.in",
            "https://mail.zoho.com.au",
            "https://mail.zoho.jp",
            "https://mail.zohocloud.ca",
            "https://mail.zoho.com.cn",
            "https://mail.zoho.ae",
            "https://mail.zoho.sa",
        )

        fun requireOfficialOrigin(value: String): String {
            val uri = runCatching { URI(value.trim()) }.getOrElse {
                throw IllegalArgumentException("Zoho Mail API origin must be provider-verified and official")
            }
            require(uri.scheme.equals("https", ignoreCase = true)) { "Zoho Mail API requires HTTPS" }
            require(uri.userInfo == null && uri.port == -1 && uri.query == null && uri.fragment == null) {
                "Zoho Mail API origin must not contain credentials, a port, a query, or a fragment"
            }
            require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") { "Zoho Mail API origin must not contain a path" }
            val normalized = "https://${uri.host?.lowercase(Locale.ROOT) ?: ""}"
            require(normalized in OFFICIAL_ORIGINS) { "Zoho Mail API origin must be provider-verified and official" }
            return normalized
        }
    }
}

internal data class ZohoFolder(val id: String, val name: String, val type: String?, val path: String?)
internal data class ZohoSummary(
    val id: String,
    val receivedTime: Long?,
    val subject: String?,
    val read: Boolean,
    val flagged: Boolean,
    val threadId: String?,
    val size: Long?,
    val from: String?,
    val folderId: String?,
    val sender: String? = null,
    val to: String? = null,
)
internal data class ZohoMessage(
    val id: String,
    val subject: String?,
    val content: String,
    val from: String?,
    val to: String?,
    val sentTime: Long?,
    val messageIdHeader: String?,
    val dateHeader: String? = null,
    val sender: String? = null,
)
internal data class ZohoAttachment(
    val id: String,
    val name: String?,
    val contentType: String?,
    val size: Long?,
    val inline: Boolean,
    val contentId: String?,
)
internal data class ZohoUploadedAttachment(val name: String, val path: String, val storeName: String)
internal data class ZohoPage(val messages: List<ZohoSummary>, val start: Int, val requestedLimit: Int) {
    val hasMore: Boolean get() = messages.size >= requestedLimit
    val nextStart: Int get() = start + messages.size
}
internal class ZohoMailApiException(val statusCode: Int, message: String) : IOException(message)

internal class ZohoSendOutcomeUnknownException(cause: Throwable) : MessagingException(
    message = "Zoho Mail did not confirm whether the message was accepted. Check Sent before retrying.",
    isPermanentFailure = true,
    cause = cause,
)
