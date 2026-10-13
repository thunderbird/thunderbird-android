package com.fsck.k9.backend.graph

import com.fsck.k9.backend.api.TransientApiRequestRetry
import com.fsck.k9.mail.Body
import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.logging.Logger
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

internal data class GraphPage<T>(
    val values: List<T>,
    val nextLink: String?,
    val deltaLink: String?,
)

internal data class GraphMailFolder(
    val id: String,
    val displayName: String,
    val path: String,
    val childFolderCount: Int,
    val wellKnownName: String? = null,
)

internal data class GraphEmailAddress(
    val name: String?,
    val address: String?,
)

internal data class GraphMessage(
    val id: String,
    val receivedDateTime: String?,
    val sentDateTime: String?,
    val lastModifiedDateTime: String?,
    val subject: String?,
    val internetMessageId: String?,
    val from: GraphEmailAddress?,
    val sender: GraphEmailAddress?,
    val toRecipients: List<GraphEmailAddress>,
    val ccRecipients: List<GraphEmailAddress>,
    val bccRecipients: List<GraphEmailAddress>,
    val replyTo: List<GraphEmailAddress>,
    val bodyContent: String?,
    val bodyContentType: String?,
    val bodyPreview: String?,
    val isRead: Boolean,
    val isFlagged: Boolean,
    val hasAttachments: Boolean,
    val removed: Boolean,
)

internal data class GraphAttachment(
    val id: String,
    val name: String?,
    val contentType: String?,
    val size: Long?,
    val isInline: Boolean,
    val contentId: String?,
    val type: String?,
)

private class GraphCountingInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {
    var bytesRead = 0L
        private set

    override fun read(): Int = super.read().also { if (it != -1) bytesRead++ }

    override fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) bytesRead += it }

    override fun skip(count: Long): Long = super.skip(count).also { bytesRead += it }
}

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404
private const val HTTP_GONE = 410
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_BAD_GATEWAY = 502
private const val HTTP_SERVICE_UNAVAILABLE = 503
internal const val GRAPH_HTTP_BAD_GATEWAY = HTTP_BAD_GATEWAY
private const val GRAPH_API_LOG_TAG = "GraphApiClient"
private const val MAX_LOGGED_ERROR_BODY_BYTES = 16 * 1024L

internal open class GraphApiException(
    val statusCode: Int,
    message: String,
    val retryAfterSeconds: Long? = null,
) : IOException(message)

internal class GraphSendOutcomeUnknownException(cause: IOException) : MessagingException(
    message = "Microsoft Graph did not confirm whether the message was accepted. Check Sent Items before retrying.",
    isPermanentFailure = true,
    cause = cause,
)

internal class GraphDeltaExpiredException : GraphApiException(
    statusCode = HTTP_GONE,
    message = "Microsoft Graph delta state expired",
)

/** A small REST client deliberately limited to the global Microsoft Graph v1.0 endpoint. */
@Suppress("TooManyFunctions")
internal class GraphApiClient(
    okHttpClient: OkHttpClient,
    private val tokenProvider: OAuth2TokenProvider,
    private val logger: Logger,
    private val baseUrl: HttpUrl = GRAPH_BASE_URL,
) {
    private val apiHttpClient = okHttpClient.newBuilder()
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
        .build()
    private val moshi = Moshi.Builder().build()
    private val mapAdapter: JsonAdapter<Map<*, *>> = moshi.adapter(Map::class.java)
    private val sendMailClient = apiHttpClient.newBuilder()
        .writeTimeout(SEND_MAIL_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun listFolders(): List<GraphMailFolder> {
        val knownFolderIds = loadWellKnownFolderIds()
        val knownNamesById = knownFolderIds.entries.associate { (name, id) -> id to name }
        val visited = mutableSetOf<String>()
        val result = mutableListOf<GraphMailFolder>()
        val pending = ArrayDeque<Pair<String?, String>>()
        pending.add(null to "")

        while (pending.isNotEmpty()) {
            val (parentId, parentPath) = pending.removeFirst()
            var nextLink: String? = null
            do {
                val page = getFolderPage(parentId, nextLink)
                for (folder in page.values) {
                    if (!visited.add(folder.id)) continue
                    val displayName = folder.displayName.ifBlank { folder.id }
                    val fullPath = if (parentPath.isEmpty()) displayName else "$parentPath/$displayName"
                    result += GraphMailFolder(
                        id = folder.id,
                        displayName = displayName,
                        path = fullPath,
                        childFolderCount = folder.childFolderCount,
                        wellKnownName = knownNamesById[folder.id],
                    )
                    if (folder.childFolderCount > 0) {
                        pending.add(folder.id to fullPath)
                    }
                }
                nextLink = page.nextLink
            } while (nextLink != null)
        }

        return result
    }

    fun sendMessage(message: Message) {
        val mimeOutput = ByteArrayOutputStream()
        message.writeTo(mimeOutput)
        val body = Base64.getEncoder().encodeToString(mimeOutput.toByteArray())
            .toRequestBody("text/plain".toMediaType())
        try {
            execute(
                url = endpoint("me", "sendMail"),
                accept = "application/json",
                httpMethod = "POST",
                requestBody = body,
                client = sendMailClient,
            ).use { }
        } catch (e: GraphApiException) {
            throw e
        } catch (e: IOException) {
            // A transport failure without an HTTP response cannot establish whether Graph accepted the message.
            // Preserve it in the Outbox as a permanent error so the controller won't automatically resend it.
            throw GraphSendOutcomeUnknownException(e)
        }
    }

    fun setMessageRead(messageId: String, isRead: Boolean) {
        patchMessage(messageId, mapOf("isRead" to isRead))
    }

    fun setMessageFlagged(messageId: String, isFlagged: Boolean) {
        patchMessage(
            messageId,
            mapOf("flag" to mapOf("flagStatus" to if (isFlagged) "flagged" else "notFlagged")),
        )
    }

    fun deleteMessage(messageId: String) {
        execute(
            url = endpoint("me", "messages", messageId),
            accept = "application/json",
            httpMethod = "DELETE",
        ).use { }
    }

    fun listMessageIds(folderId: String, unreadOnly: Boolean = false): List<String> {
        val result = mutableListOf<String>()
        var nextLink: String? = null
        do {
            val url = if (nextLink != null) {
                continuationUrl(nextLink)
            } else {
                endpoint("me", "mailFolders", folderId, "messages")
                    .newBuilder()
                    .addQueryParameter("\$select", if (unreadOnly) "id,isRead" else "id")
                    .addQueryParameter("\$top", MESSAGE_PAGE_SIZE.toString())
                    .apply {
                        if (unreadOnly) addQueryParameter("\$filter", "isRead eq false")
                    }
                    .build()
            }
            val page = getMessagePage(url)
            result += page.values.filter { !unreadOnly || !it.isRead }.map { it.id }
            nextLink = page.nextLink
        } while (nextLink != null)
        return result
    }

    fun moveMessage(messageId: String, destinationFolderId: String): String =
        performMessageAction(messageId, "move", destinationFolderId)

    fun copyMessage(messageId: String, destinationFolderId: String): String =
        performMessageAction(messageId, "copy", destinationFolderId)

    private fun performMessageAction(messageId: String, action: String, destinationFolderId: String): String {
        val body = jsonBody(mapOf("destinationId" to destinationFolderId))
        val response = execute(
            url = endpoint("me", "messages", messageId, action),
            accept = "application/json",
            httpMethod = "POST",
            requestBody = body,
        )
        response.use {
            return parseObject(it.body.string()).string("id")
                ?: throw GraphApiException(
                    GRAPH_HTTP_BAD_GATEWAY,
                    "Microsoft Graph did not return the copied or moved message ID",
                )
        }
    }

    private fun patchMessage(messageId: String, properties: Map<String, Any?>) {
        execute(
            url = endpoint("me", "messages", messageId),
            accept = "application/json",
            httpMethod = "PATCH",
            requestBody = jsonBody(properties),
        ).use { }
    }

    private fun jsonBody(values: Map<String, Any?>): RequestBody =
        mapAdapter.toJson(values).toRequestBody("application/json".toMediaType())

    fun getLatestMessages(folderId: String, limit: Int): GraphPage<GraphMessage> {
        val url = endpoint("me", "mailFolders", folderId, "messages")
            .newBuilder()
            .addQueryParameter("\$select", MESSAGE_SUMMARY_SELECT)
            .addQueryParameter("\$orderby", "receivedDateTime desc")
            .addQueryParameter("\$top", limit.toString())
            .build()
        return getMessagePage(url)
    }

    fun getMessageDeltaPage(folderId: String, cutoff: Instant?): GraphPage<GraphMessage> {
        val builder = endpoint("me", "mailFolders", folderId, "messages", "delta")
            .newBuilder()
            .addQueryParameter("\$select", MESSAGE_SUMMARY_SELECT)
            .addQueryParameter("\$orderby", "receivedDateTime desc")
            .addQueryParameter("\$top", DELTA_PAGE_SIZE.toString())
        if (cutoff != null) {
            builder.addQueryParameter(
                "\$filter",
                "receivedDateTime ge ${DateTimeFormatter.ISO_INSTANT.format(cutoff)}",
            )
        }
        return getMessagePage(builder.build())
    }

    fun getMessagePage(nextLink: String): GraphPage<GraphMessage> {
        return getMessagePage(continuationUrl(nextLink))
    }

    fun getMessage(folderId: String, messageId: String): GraphMessage {
        val url = endpoint("me", "mailFolders", folderId, "messages", messageId)
            .newBuilder()
            .addQueryParameter("\$select", MESSAGE_DETAIL_SELECT)
            .build()
        val json = getJson(url)
        return parseMessage(parseObject(json))
    }

    fun listAttachments(messageId: String): List<GraphAttachment> {
        val result = mutableListOf<GraphAttachment>()
        var nextLink: String? = null
        do {
            val url = if (nextLink == null) {
                endpoint("me", "messages", messageId, "attachments")
                    .newBuilder()
                    .addQueryParameter("\$select", ATTACHMENT_METADATA_SELECT)
                    .addQueryParameter("\$top", ATTACHMENT_PAGE_SIZE.toString())
                    .build()
            } else {
                continuationUrl(nextLink)
            }
            val page = getCollection(url, ::parseAttachment)
            result += page.values
            nextLink = page.nextLink
        } while (nextLink != null)
        return result.map { attachment ->
            if (attachment.isInline && attachment.type.isFileAttachment()) {
                attachment.copy(contentId = getFileAttachmentContentId(messageId, attachment.id))
            } else {
                attachment
            }
        }
    }

    private fun getFileAttachmentContentId(messageId: String, attachmentId: String): String? {
        val url = endpoint("me", "messages", messageId, "attachments", attachmentId)
            .newBuilder()
            .addQueryParameter("\$select", FILE_ATTACHMENT_CONTENT_ID_SELECT)
            .build()
        return parseObject(getJson(url)).string("contentId")
    }

    private fun String?.isFileAttachment(): Boolean =
        this?.endsWith("fileAttachment", ignoreCase = true) == true

    fun downloadAttachment(
        messageId: String,
        attachmentId: String,
        partContentType: String,
        bodyFactory: BodyFactory,
        metadataSize: Long? = null,
    ): Body {
        val url = endpoint("me", "messages", messageId, "attachments", attachmentId, "\$value")
        val response = execute(url, accept = "*/*")
        response.use {
            val responseBody = it.body
            val contentLength = responseBody.contentLength()
            val inputStream = GraphCountingInputStream(responseBody.byteStream())
            val downloadedBody = bodyFactory.createBody("binary", partContentType, inputStream)
            val bytesRead = inputStream.bytesRead
            if (metadataSize != null && bytesRead != metadataSize) {
                logger.debug(GRAPH_API_LOG_TAG) {
                    "Graph attachment payload size differs from metadata: " +
                        "metadataBytes=$metadataSize downloadedBytes=$bytesRead"
                }
            }
            if (contentLength >= 0 && bytesRead != contentLength) {
                releaseBody(downloadedBody)
                throw MessagingException(
                    "Microsoft Graph attachment Content-Length mismatch: expected $contentLength bytes, " +
                        "received $bytesRead",
                )
            }
            return downloadedBody
        }
    }

    private fun releaseBody(body: Body) {
        if (body is BinaryTempFileBody) {
            body.file.delete()
            return
        }

        try {
            body.inputStream.close()
        } catch (_: Exception) {
            // Preserve the size-mismatch error while making a best-effort cleanup of the temporary body.
        }
    }

    private fun getFolderPage(parentFolderId: String?, nextLink: String?): GraphPage<GraphFolderDto> {
        val url = if (nextLink != null) {
            continuationUrl(nextLink)
        } else {
            val builder = if (parentFolderId == null) {
                endpoint("me", "mailFolders").newBuilder()
            } else {
                endpoint("me", "mailFolders", parentFolderId, "childFolders").newBuilder()
            }
            builder
                .addQueryParameter("\$select", FOLDER_SELECT)
                .addQueryParameter("\$top", FOLDER_PAGE_SIZE.toString())
                .build()
        }
        return getCollection(url, ::parseFolder)
    }

    private fun loadWellKnownFolderIds(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (wellKnownName in WELL_KNOWN_FOLDERS) {
            val url = endpoint("me", "mailFolders", wellKnownName)
                .newBuilder()
                .addQueryParameter("\$select", "id")
                .build()
            try {
                val folder = parseFolder(parseObject(getJson(url)))
                result[wellKnownName] = folder.id
            } catch (e: GraphApiException) {
                if (e.statusCode != HTTP_NOT_FOUND) throw e
            }
        }
        return result
    }

    private fun getMessagePage(url: HttpUrl): GraphPage<GraphMessage> =
        getCollection(url, ::parseMessage) { it.id }

    private fun <T> getCollection(
        url: HttpUrl,
        mapper: (Map<String, Any?>) -> T,
        messageId: ((T) -> String)? = null,
    ): GraphPage<T> {
        val root = parseObject(getJson(url))
        val responseValues = root["value"] as? List<*>
        val values = responseValues.orEmpty().mapNotNull { value ->
            asMap(value)?.let(mapper)
        }
        logger.debug(GRAPH_API_LOG_TAG) {
            val messageIds = messageId?.let { ids ->
                values.joinToString(prefix = "[", postfix = "]") { ids(it).escapeForLog() }
            }
            "Graph API collection result path=${url.encodedPath} " +
                "itemCount=${responseValues?.size ?: 0} parsedCount=${values.size}" +
                (messageIds?.let { " messageIds=$it" } ?: "") +
                " hasNextPage=${root["@odata.nextLink"] != null} hasDeltaLink=${root["@odata.deltaLink"] != null}"
        }
        return GraphPage(
            values = values,
            nextLink = root["@odata.nextLink"] as? String,
            deltaLink = root["@odata.deltaLink"] as? String,
        )
    }

    private fun getJson(url: HttpUrl): String {
        val response = execute(url, accept = "application/json")
        response.use {
            return it.body.string()
        }
    }

    @Suppress("ThrowsCount")
    private fun execute(
        url: HttpUrl,
        accept: String,
        httpMethod: String = "GET",
        requestBody: RequestBody? = null,
        client: OkHttpClient = apiHttpClient,
    ): Response {
        val safeUrl = checkedUrl(url)
        var attempt = 0
        while (attempt < MAX_AUTH_ATTEMPTS) {
            val clientRequestId = UUID.randomUUID().toString()
            val token = getAccessToken(safeUrl, httpMethod, attempt)
            val request = buildRequest(safeUrl, accept, httpMethod, requestBody, token, clientRequestId)
            val response = executeWithTransientRetry(client, request, clientRequestId)
            if (response.code == HTTP_UNAUTHORIZED && attempt + 1 < MAX_AUTH_ATTEMPTS) {
                response.close()
                tokenProvider.invalidateToken()
                attempt++
                continue
            }
            if (!response.isSuccessful) throwHttpError(response)
            return response
        }
        throw AuthenticationFailedException("Microsoft Graph authentication failed")
    }

    private fun getAccessToken(url: HttpUrl, httpMethod: String, attempt: Int): String = try {
        tokenProvider.getToken(OAuth2TokenProvider.OAUTH2_TIMEOUT.toLong())
    } catch (e: AuthenticationFailedException) {
        logger.warn(GRAPH_API_LOG_TAG, e) {
            "Graph API token acquisition failed method=$httpMethod path=${url.encodedPath} attempt=${attempt + 1}"
        }
        throw AuthenticationFailedException("Unable to obtain a Microsoft Graph access token", e)
    }

    private fun buildRequest(
        url: HttpUrl,
        accept: String,
        httpMethod: String,
        requestBody: RequestBody?,
        token: String,
        clientRequestId: String,
    ): Request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", accept)
        .header("Prefer", "IdType=\"ImmutableId\"")
        .header("client-request-id", clientRequestId)
        .header("return-client-request-id", "true")
        .method(httpMethod, requestBody)
        .build()

    private fun logRequest(request: Request, attempt: Int, clientRequestId: String) {
        logger.debug(GRAPH_API_LOG_TAG) {
            "Graph API request method=${request.method} path=${request.url.encodedPath} " +
                "queryKeys=${request.url.queryParameterNames.joinToString(",")} " +
                "attempt=${attempt + 1} clientRequestId=$clientRequestId"
        }
    }

    private fun executeRequest(client: OkHttpClient, request: Request, clientRequestId: String): Response {
        val startedAtNanos = System.nanoTime()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            logger.warn(GRAPH_API_LOG_TAG, e) {
                "Graph API transport failure method=${request.method} path=${request.url.encodedPath} " +
                    "clientRequestId=$clientRequestId"
            }
            throw e
        }
        logResponse(request, response, clientRequestId, startedAtNanos)
        return response
    }

    private fun executeWithTransientRetry(client: OkHttpClient, request: Request, clientRequestId: String): Response {
        for (attempt in 1..TransientApiRequestRetry.MAX_ATTEMPTS) {
            logRequest(request, attempt - 1, clientRequestId)
            val response = try {
                executeRequest(client, request, clientRequestId)
            } catch (error: IOException) {
                if (!TransientApiRequestRetry.shouldRetryTransport(request.method, attempt)) throw error
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
        throw IOException("Microsoft Graph REST request retry limit reached")
    }

    private fun logResponse(request: Request, response: Response, clientRequestId: String, startedAtNanos: Long) {
        val errorCode = if (response.isSuccessful) null else graphErrorCode(response)
        logger.debug(GRAPH_API_LOG_TAG) {
            "Graph API response method=${request.method} path=${request.url.encodedPath} " +
                "status=${response.code} result=${if (response.isSuccessful) "success" else "failure"} " +
                "durationMs=${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)} " +
                "clientRequestId=$clientRequestId " +
                "echoedClientRequestId=${response.header("client-request-id")?.escapeForLog() ?: "none"} " +
                "requestId=${response.header("request-id")?.escapeForLog() ?: "none"} " +
                "errorCode=${errorCode?.escapeForLog() ?: "none"} " +
                    "retryAfter=${response.header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER) ?: response.header("Retry-After") ?: "none"}"
        }
    }

    @Suppress("ThrowsCount")
    private fun throwHttpError(response: Response): Nothing {
        val statusCode = response.code
        val retryAfter = (response.header(TransientApiRequestRetry.ORIGINAL_RETRY_AFTER_HEADER)
            ?: response.header("Retry-After"))?.toLongOrNull()
        response.close()
        when (statusCode) {
            HTTP_UNAUTHORIZED -> throw AuthenticationFailedException("Microsoft Graph rejected the access token")
            HTTP_GONE -> throw GraphDeltaExpiredException()
            HTTP_TOO_MANY_REQUESTS, HTTP_SERVICE_UNAVAILABLE -> throw GraphApiException(
                statusCode = statusCode,
                message = "Microsoft Graph throttled the request",
                retryAfterSeconds = retryAfter,
            )
            else -> throw GraphApiException(statusCode, "Microsoft Graph request failed (HTTP $statusCode)")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun graphErrorCode(response: Response): String? = try {
        parseObject(response.peekBody(MAX_LOGGED_ERROR_BODY_BYTES).string())
            .map("error")
            ?.string("code")
    } catch (_: Exception) {
        null
    }

    private fun endpoint(vararg segments: String): HttpUrl {
        val builder = baseUrl.newBuilder()
        segments.forEach(builder::addPathSegment)
        return builder.build()
    }

    private fun continuationUrl(link: String): HttpUrl {
        val url = link.toHttpUrlOrNull()
            ?: throw GraphApiException(HTTP_BAD_REQUEST, "Invalid Microsoft Graph continuation link")
        return checkedUrl(url)
    }

    private fun checkedUrl(url: HttpUrl): HttpUrl {
        val isSafe = hasAllowedOrigin(url) && hasAllowedApiPath(url) && hasNoCredentialsOrFragment(url)
        if (!isSafe) {
            throw GraphApiException(HTTP_BAD_REQUEST, "Rejected an unsafe Microsoft Graph URL")
        }
        return url
    }

    private fun hasAllowedOrigin(url: HttpUrl): Boolean =
        url.scheme == baseUrl.scheme && url.host == baseUrl.host && url.port == baseUrl.port

    private fun hasAllowedApiPath(url: HttpUrl): Boolean = url.encodedPath.startsWith(baseUrl.encodedPath)

    private fun hasNoCredentialsOrFragment(url: HttpUrl): Boolean =
        url.username.isEmpty() && url.password.isEmpty() && url.fragment == null

    private fun parseFolder(value: Map<String, Any?>) = GraphFolderDto(
        id = value.string("id") ?: throw GraphApiException(GRAPH_HTTP_BAD_GATEWAY, "Microsoft Graph folder had no ID"),
        displayName = value.string("displayName").orEmpty(),
        childFolderCount = value.number("childFolderCount")?.toInt() ?: 0,
    )

    private fun parseMessage(value: Map<String, Any?>): GraphMessage {
        val body = value.map("body")
        val flag = value.map("flag")
        return GraphMessage(
            id = value.string("id") ?: throw GraphApiException(
                GRAPH_HTTP_BAD_GATEWAY,
                "Microsoft Graph message had no ID",
            ),
            receivedDateTime = value.string("receivedDateTime"),
            sentDateTime = value.string("sentDateTime"),
            lastModifiedDateTime = value.string("lastModifiedDateTime"),
            subject = value.string("subject"),
            internetMessageId = value.string("internetMessageId"),
            from = value.map("from")?.map("emailAddress")?.toEmailAddress(),
            sender = value.map("sender")?.map("emailAddress")?.toEmailAddress(),
            toRecipients = value.emailAddresses("toRecipients"),
            ccRecipients = value.emailAddresses("ccRecipients"),
            bccRecipients = value.emailAddresses("bccRecipients"),
            replyTo = value.emailAddresses("replyTo"),
            bodyContent = body?.string("content"),
            bodyContentType = body?.string("contentType"),
            bodyPreview = value.string("bodyPreview"),
            isRead = value.boolean("isRead") ?: false,
            isFlagged = flag?.string("flagStatus") == "flagged",
            hasAttachments = value.boolean("hasAttachments") ?: false,
            removed = value.containsKey("@removed"),
        )
    }

    private fun parseAttachment(value: Map<String, Any?>) = GraphAttachment(
        id = value.string("id") ?: throw GraphApiException(
            GRAPH_HTTP_BAD_GATEWAY,
            "Microsoft Graph attachment had no ID",
        ),
        name = value.string("name"),
        contentType = value.string("contentType"),
        size = value.number("size")?.toLong(),
        isInline = value.boolean("isInline") ?: false,
        contentId = value.string("contentId"),
        type = value.string("@odata.type"),
    )

    private fun parseObject(json: String): Map<String, Any?> {
        val value = mapAdapter.fromJson(json)
            ?: throw GraphApiException(GRAPH_HTTP_BAD_GATEWAY, "Invalid Microsoft Graph JSON response")
        return asMap(value)
            ?: throw GraphApiException(GRAPH_HTTP_BAD_GATEWAY, "Expected a Microsoft Graph JSON object")
    }

    private fun asMap(value: Any?): Map<String, Any?>? = (value as? Map<*, *>)?.entries?.mapNotNull { (key, item) ->
        (key as? String)?.let { it to item }
    }?.toMap()

    private fun Map<String, Any?>.string(name: String): String? = this[name] as? String
    private fun Map<String, Any?>.number(name: String): Number? = this[name] as? Number
    private fun Map<String, Any?>.boolean(name: String): Boolean? = this[name] as? Boolean
    private fun Map<String, Any?>.map(name: String): Map<String, Any?>? = asMap(this[name])
    private fun Map<String, Any?>.emailAddresses(name: String): List<GraphEmailAddress> =
        (this[name] as? List<*>).orEmpty().mapNotNull { asMap(it)?.get("emailAddress")?.let(::asMap)?.toEmailAddress() }

    private fun Map<String, Any?>.toEmailAddress() = GraphEmailAddress(
        name = string("name"),
        address = string("address"),
    )

    private data class GraphFolderDto(val id: String, val displayName: String, val childFolderCount: Int)

    private fun String.escapeForLog(): String =
        replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")

    companion object {
        private val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0/".toHttpUrl()
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 45L
        private const val WRITE_TIMEOUT_SECONDS = 60L
        private const val MAX_AUTH_ATTEMPTS = 2
        private const val DELTA_PAGE_SIZE = 100
        private const val FOLDER_PAGE_SIZE = 100
        private const val ATTACHMENT_PAGE_SIZE = 100
        private const val MESSAGE_PAGE_SIZE = 100
        private const val SEND_MAIL_WRITE_TIMEOUT_SECONDS = 120L
        private const val FOLDER_SELECT =
            "id,displayName,parentFolderId,childFolderCount," + "totalItemCount,unreadItemCount"
        private const val MESSAGE_SUMMARY_SELECT =
            "id,receivedDateTime,sentDateTime,lastModifiedDateTime," +
                "isRead,flag,hasAttachments,internetMessageId,subject"
        private const val MESSAGE_DETAIL_SELECT =
            MESSAGE_SUMMARY_SELECT + ",from,sender,toRecipients," +
                "ccRecipients,bccRecipients,replyTo,body,bodyPreview"
        private const val ATTACHMENT_METADATA_SELECT = "id,name,contentType,size,isInline"
        private const val FILE_ATTACHMENT_CONTENT_ID_SELECT = "id,contentId"
        private val WELL_KNOWN_FOLDERS = listOf(
            "inbox",
            "sentitems",
            "drafts",
            "deleteditems",
            "junkemail",
            "archive",
        )
    }
}
