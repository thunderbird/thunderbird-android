package com.fsck.k9.backend.gmailapi

import com.fsck.k9.backend.api.TransientApiRequestRetry
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal data class GmailLabel(val id: String, val name: String, val type: String?)
internal data class GmailRef(val id: String, val threadId: String?, val labelIds: List<String>)
internal data class GmailMessage(
    val id: String,
    val threadId: String?,
    val labelIds: List<String>,
    val payload: Map<String, Any?>? = null,
    val sizeEstimate: Long? = null,
    val internalDateMillis: Long? = null,
)
internal data class GmailPage(val messages: List<GmailRef>, val nextPageToken: String?, val resultSizeEstimate: Int?)
internal data class GmailHistoryChanges(
    val addedMessageIds: List<String>,
    val changedMessageIds: Set<String>,
    val deletedMessageIds: Set<String>,
    val latestHistoryId: String?,
    val requiresNewerSearch: Boolean = false,
) {
    companion object {
        fun empty(latestHistoryId: String?, requiresNewerSearch: Boolean = false) = GmailHistoryChanges(
            addedMessageIds = emptyList(),
            changedMessageIds = emptySet(),
            deletedMessageIds = emptySet(),
            latestHistoryId = latestHistoryId,
            requiresNewerSearch = requiresNewerSearch,
        )
    }
}

internal class GmailApiException(
    val statusCode: Int,
    message: String,
    val reason: String? = null,
) : IOException(message)

internal class GmailSendOutcomeUnknownException(cause: Throwable) : MessagingException(
    message = "Gmail did not confirm whether the message was accepted. Check Sent before retrying.",
    isPermanentFailure = true,
    cause = cause,
)

/** Gmail v1 REST only. It intentionally never exposes tokens or response bodies in errors/logs. */
internal class GmailApiClient(
    client: OkHttpClient,
    private val tokenProvider: OAuth2TokenProvider,
    private val logger: Logger,
    private val baseUrl: HttpUrl = "https://gmail.googleapis.com/gmail/v1/users".toHttpUrl(),
) {
    private val client = client.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val retryAfterHeader = response.header("Retry-After")
            if (response.code == HTTP_SERVICE_UNAVAILABLE && retryAfterHeader?.trim()?.toLongOrNull() == 0L) {
                response.newBuilder()
                    .header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER, requireNotNull(retryAfterHeader))
                    .removeHeader("Retry-After")
                    .build()
            } else {
                response
            }
        }
        .build()
    private val moshi = Moshi.Builder().build()
    private val mapAdapter: JsonAdapter<Map<*, *>> = moshi.adapter(Map::class.java)

    fun labels(): List<GmailLabel> = get("me/labels").objList("labels").mapNotNull {
        val id = it.string("id") ?: return@mapNotNull null
        GmailLabel(id, it.string("name") ?: id, it.string("type"))
    }

    fun listMessages(
        labelId: String,
        pageToken: String? = null,
        maxResults: Int = 100,
        query: String? = null,
    ): GmailPage {
        val url = endpoint("me/messages").newBuilder()
            .apply {
                if (labelId != ALL_MAIL_LABEL_ID) addQueryParameter("labelIds", labelId)
                addQueryParameter("maxResults", maxResults.coerceIn(1, 500).toString())
                if (labelId == ALL_MAIL_LABEL_ID) addQueryParameter("q", combineQueries(ALL_MAIL_QUERY, query))
                else query?.takeIf(String::isNotBlank)?.let { addQueryParameter("q", it) }
                if (labelId == "SPAM" || labelId == "TRASH") addQueryParameter("includeSpamTrash", "true")
                pageToken?.let { addQueryParameter("pageToken", it) }
            }
            .build()
        val result = get(url)
        return GmailPage(
            messages = result.objList("messages").mapNotNull { message ->
                message.string("id")?.let { GmailRef(it, message.string("threadId"), message.stringList("labelIds")) }
            },
            nextPageToken = result.string("nextPageToken"),
            resultSizeEstimate = result.int("resultSizeEstimate"),
        )
    }

    fun history(startHistoryId: String): GmailHistoryChanges {
        val addedMessageIds = mutableListOf<String>()
        val changedMessageIds = mutableSetOf<String>()
        val deletedMessageIds = mutableSetOf<String>()
        var pageToken: String? = null
        var latestHistoryId: String? = null
        do {
            val url = endpoint("me/history").newBuilder()
                .addQueryParameter("startHistoryId", startHistoryId)
                .addQueryParameter("maxResults", "500")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()
            val page = get(url)
            latestHistoryId = page.string("historyId") ?: latestHistoryId
            page.objList("history").forEach { record ->
                record.objList("messagesAdded").forEach { item ->
                    item.historyMessageId()?.let { id ->
                        addedMessageIds += id
                        changedMessageIds += id
                    }
                }
                record.objList("messagesDeleted").forEach { item ->
                    item.historyMessageId()?.let { id ->
                        changedMessageIds += id
                        deletedMessageIds += id
                    }
                }
                listOf("labelsAdded", "labelsRemoved").forEach { eventName ->
                    record.objList(eventName).forEach { item ->
                        item.historyMessageId()?.let(changedMessageIds::add)
                    }
                }
            }
            pageToken = page.string("nextPageToken")
        } while (pageToken != null)
        return GmailHistoryChanges(
            addedMessageIds = addedMessageIds.distinct(),
            changedMessageIds = changedMessageIds,
            deletedMessageIds = deletedMessageIds,
            latestHistoryId = latestHistoryId,
        )
    }

    fun getMessage(id: String, format: String = "full"): GmailMessage {
        val result = get(endpoint("me/messages", id).newBuilder().addQueryParameter("format", format).build())
        return GmailMessage(
            id = result.string("id") ?: id,
            threadId = result.string("threadId"),
            labelIds = result.stringList("labelIds"),
            payload = result["payload"] as? Map<String, Any?>,
            sizeEstimate = result.long("sizeEstimate"),
            internalDateMillis = result.string("internalDate")?.toLongOrNull(),
        )
    }

    fun rawMessage(id: String): ByteArray = decode(get(endpoint("me/messages", id).newBuilder().addQueryParameter("format", "raw").build()).string("raw").orEmpty())

    fun attachment(messageId: String, attachmentId: String): ByteArray =
        decode(get(endpoint("me/messages", messageId, "attachments", attachmentId)).string("data").orEmpty())

    fun modify(id: String, add: List<String> = emptyList(), remove: List<String> = emptyList()) {
        post(endpoint("me/messages", id, "modify"), mapOf("addLabelIds" to add, "removeLabelIds" to remove))
    }

    fun trash(id: String) {
        post(endpoint("me/messages", id, "trash"), emptyMap<String, Any>())
    }

    fun deleteForever(id: String) {
        request(endpoint("me/messages", id), "DELETE", null).use { }
    }

    fun send(message: Message) {
        val bytes = ByteArrayOutputStream().also { message.writeTo(it) }.toByteArray()
        val body = mutableMapOf<String, Any>("raw" to Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
        val replyMessageId = message.getHeader("In-Reply-To").firstOrNull()?.let(::extractMessageId)
            ?: message.getHeader("References").lastOrNull()?.let(::extractMessageId)
        replyMessageId?.let(::findThreadIdByInternetMessageId)?.let { body["threadId"] = it }
        try {
            post(endpoint("me/messages", "send"), body)
        } catch (error: AuthenticationFailedException) {
            throw error
        } catch (error: IOException) {
            throw toSendFailure(error)
        }
    }

    fun findMessageIdByInternetMessageId(messageId: String): String? =
        findByInternetMessageId(messageId)?.id

    private fun findThreadIdByInternetMessageId(messageId: String): String? =
        findByInternetMessageId(messageId)?.threadId

    private fun findByInternetMessageId(messageId: String): GmailRef? {
        val clean = extractMessageId(messageId) ?: return null
        if (!MESSAGE_ID_TOKEN.matches(clean)) return null
        val result = get(
            endpoint("me/messages").newBuilder()
                .addQueryParameter("q", "rfc822msgid:$clean")
                .addQueryParameter("maxResults", "1")
                .build(),
        )
        return result.objList("messages").firstOrNull()?.let { message ->
            message.string("id")?.let { GmailRef(it, message.string("threadId"), message.stringList("labelIds")) }
        }
    }

    fun copy(id: String, targetLabel: String): String {
        val body = if (targetLabel == ALL_MAIL_LABEL_ID) {
            emptyMap()
        } else {
            mapOf("labelIds" to listOf(targetLabel))
        }
        val response = postForObject(endpoint("me/messages", id, "copy"), body)
        return response.string("id") ?: throw GmailApiException(502, "Gmail did not return the copied message ID")
    }

    fun currentHistoryId(): String? = get(endpoint("me/profile")).string("historyId")

    fun createMime(message: GmailMessage): ByteArray = MimeBuilder.build(message.payload ?: emptyMap())

    private fun get(path: String): Map<String, Any?> = get(endpoint(*path.split('/').toTypedArray()))

    private fun get(url: HttpUrl): Map<String, Any?> = request(url, "GET", null).use { parse(it.body.string()) }

    private fun post(url: HttpUrl, body: Map<String, Any?>) {
        request(url, "POST", mapAdapter.toJson(body).toRequestBody("application/json".toMediaType())).use { }
    }

    private fun postForObject(url: HttpUrl, body: Map<String, Any?>): Map<String, Any?> =
        request(url, "POST", mapAdapter.toJson(body).toRequestBody("application/json".toMediaType())).use { parse(it.body.string()) }

    private fun request(url: HttpUrl, method: String, body: okhttp3.RequestBody?): okhttp3.Response {
        for (authorizationAttempt in 1..MAX_AUTH_ATTEMPTS) {
            val token = tokenProvider.getToken(OAuth2TokenProvider.OAUTH2_TIMEOUT.toLong())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .method(method, body)
                .build()
            val response = executeWithTransientRetry(request)
            if (response.code == HTTP_UNAUTHORIZED) {
                response.close()
                if (authorizationAttempt < MAX_AUTH_ATTEMPTS) {
                    tokenProvider.invalidateToken()
                }
            } else {
                return validateResponse(response, url)
            }
        }
        throw AuthenticationFailedException("Gmail authentication failed")
    }

    private fun validateResponse(response: okhttp3.Response, url: HttpUrl): okhttp3.Response {
        if (response.isSuccessful) return response

        val statusCode = response.code
        val reason = response.body.string().let(::errorReason)
        response.close()
        logger.warn(TAG) {
            "Gmail REST request failed status=$statusCode reason=${reason ?: "unknown"} path=${url.encodedPath}"
        }
        val reasonSuffix = reason?.let { ", reason=$it" }.orEmpty()
        throw GmailApiException(statusCode, "Gmail REST request failed ($statusCode$reasonSuffix)", reason)
    }

    private fun toSendFailure(error: IOException): Exception = when (error) {
        is GmailApiException -> {
            if (error.statusCode == HTTP_REQUEST_TIMEOUT || error.statusCode >= HTTP_INTERNAL_SERVER_ERROR) {
                GmailSendOutcomeUnknownException(error)
            } else {
                error
            }
        }

        else -> GmailSendOutcomeUnknownException(error)
    }

    private fun executeWithTransientRetry(request: Request): okhttp3.Response {
        var attempt = 1
        var lastTransportError: IOException? = null
        while (attempt <= TransientApiRequestRetry.MAX_ATTEMPTS) {
            val response = try {
                client.newCall(request).execute()
            } catch (error: IOException) {
                if (!TransientApiRequestRetry.shouldRetryTransport(request.method, attempt)) {
                    throw IOException("Gmail REST request failed", error)
                }
                lastTransportError = error
                null
            }

            if (response == null) {
                TransientApiRequestRetry.waitBeforeRetry(attempt)
            } else if (TransientApiRequestRetry.shouldRetryResponse(request.method, response.code, attempt)) {
                val retryAfter = response.header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER)
                    ?: response.header("Retry-After")
                response.close()
                TransientApiRequestRetry.waitBeforeRetry(attempt, retryAfter)
            } else {
                return response
            }

            attempt++
        }
        throw IOException("Gmail REST request retry limit reached", lastTransportError)
    }

    private fun errorReason(json: String): String? = try {
        val error = mapAdapter.fromJson(json)?.get("error") as? Map<*, *> ?: return null
        val errors = error["errors"] as? List<*> ?: return null
        val reason = (errors.firstOrNull() as? Map<*, *>)?.get("reason") as? String ?: return null
        reason.takeIf(SAFE_ERROR_REASON::matches)
    } catch (_: Exception) {
        null
    }

    private fun endpoint(vararg parts: String): HttpUrl = parts
        .flatMap { it.split('/') }
        .fold(baseUrl) { url, part -> url.newBuilder().addPathSegment(part).build() }

    private fun parse(json: String): Map<String, Any?> = (mapAdapter.fromJson(json) ?: emptyMap<Any, Any>())
        .entries.associate { entry -> entry.key.toString() to entry.value }

    private fun decode(value: String): ByteArray = try {
        Base64.getUrlDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        ByteArray(0)
    }

    private fun extractMessageId(value: String): String? =
        MESSAGE_ID_IN_HEADER.findAll(value).lastOrNull()?.groupValues?.get(1)
            ?: value.trim().takeIf(String::isNotBlank)

    private fun combineQueries(first: String, second: String?): String =
        listOfNotNull(first.takeIf(String::isNotBlank), second?.takeIf(String::isNotBlank)).joinToString(" ")

    private companion object {
        const val TAG = "GmailApi"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_SERVICE_UNAVAILABLE = 503
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_INTERNAL_SERVER_ERROR = 500
        const val MAX_AUTH_ATTEMPTS = 2
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 45L
        const val WRITE_TIMEOUT_SECONDS = 60L
        const val ALL_MAIL_LABEL_ID = "ALL_MAIL"
        const val ALL_MAIL_QUERY = "-in:trash -in:spam"
        val SAFE_ERROR_REASON = Regex("[A-Za-z][A-Za-z0-9_]{0,63}")
        val MESSAGE_ID_IN_HEADER = Regex("<([^<>]+)>")
        val MESSAGE_ID_TOKEN = Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+")
    }
}

private object MimeBuilder {
    fun build(payload: Map<String, Any?>): ByteArray = render(payload).toByteArray(StandardCharsets.ISO_8859_1)

    private fun render(payload: Map<String, Any?>): String {
        val headers = payload.objList("headers").mapNotNull { header ->
            header.string("name")?.let { it to (header.string("value") ?: "") }
        }
        val parts = payload.objList("parts")
        val body = payload["body"] as? Map<*, *>
        val attachmentId = body?.string("attachmentId")
        val bodyData = body?.string("data")
        val originalContentType = headers.firstOrNull { it.first.equals("Content-Type", true) }?.second
            ?: payload.string("mimeType")
            ?: "text/plain; charset=utf-8"
        val boundary = if (parts.isNotEmpty()) boundaryFrom(originalContentType) ?: "thunderbird-gmail-${UUID.randomUUID()}" else null
        val contentType = if (parts.isNotEmpty() && boundaryFrom(originalContentType) == null) {
            "$originalContentType; boundary=\"$boundary\""
        } else {
            originalContentType
        }
        val transferEncoding = when {
            parts.isNotEmpty() -> null
            attachmentId != null -> "binary"
            bodyData != null -> "base64"
            else -> null
        }

        return buildString {
            var contentTypeWritten = false
            headers.forEach { (name, value) ->
                when {
                    name.equals("Content-Length", true) || name.equals("Content-Transfer-Encoding", true) -> Unit
                    name.equals("Content-Type", true) -> {
                        appendHeader(name, contentType)
                        contentTypeWritten = true
                    }
                    else -> appendHeader(name, value)
                }
            }
            if (!contentTypeWritten) appendHeader("Content-Type", contentType)
            attachmentId?.let {
                val encodedId = Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(StandardCharsets.UTF_8))
                appendHeader(ATTACHMENT_ID_HEADER, encodedId)
            }
            transferEncoding?.let { appendHeader("Content-Transfer-Encoding", it) }
            append("\r\n")

            if (parts.isNotEmpty()) {
                val multipartBoundary = checkNotNull(boundary)
                parts.forEach { part ->
                    append("--").append(multipartBoundary).append("\r\n")
                    @Suppress("UNCHECKED_CAST")
                    append(render(part as Map<String, Any?>))
                    append("\r\n")
                }
                append("--").append(multipartBoundary).append("--\r\n")
            } else if (attachmentId != null) {
                // Keep the part body empty until the user requests this attachment.
            } else if (bodyData != null) {
                val encoded = Base64.getMimeEncoder(76, "\r\n".toByteArray(StandardCharsets.US_ASCII)).encodeToString(decodeBodyData(bodyData))
                append(encoded).append("\r\n")
            }
        }
    }

    private fun StringBuilder.appendHeader(name: String, value: String) {
        append(name).append(": ")
            .append(value.replace("\r", " ").replace("\n", " "))
            .append("\r\n")
    }

    private fun boundaryFrom(contentType: String): String? = BOUNDARY.find(contentType)?.let {
        (it.groups[1]?.value ?: it.groups[2]?.value)?.trim()?.takeIf(String::isNotEmpty)
    }

    private fun decodeBodyData(value: String): ByteArray = try {
        Base64.getUrlDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        ByteArray(0)
    }

    private fun Map<*, *>.string(key: String): String? = this[key] as? String

    private fun Map<*, *>.objList(key: String): List<Map<*, *>> =
        (this[key] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()

    private const val ATTACHMENT_ID_HEADER = "X-Thunderbird-Gmail-Attachment-Id"
    private val BOUNDARY = Regex("(?i)(?:^|;)\\s*boundary=(?:\"([^\"]+)\"|([^;\\s]+))")
}

private fun Map<*, *>.string(key: String): String? = this[key] as? String
private fun Map<*, *>.historyMessageId(): String? = ((this["message"] as? Map<*, *>) ?: this).string("id")
private fun Map<*, *>.int(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<*, *>.long(key: String): Long? = (this[key] as? Number)?.toLong()
private fun Map<*, *>.stringList(key: String): List<String> = (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
private fun Map<*, *>.objList(key: String): List<Map<*, *>> = (this[key] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
