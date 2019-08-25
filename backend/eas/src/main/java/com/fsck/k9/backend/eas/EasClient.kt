package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.filter.EOLConvertingOutputStream
import com.fsck.k9.mail.ssl.TrustManagerFactory
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext


class UnprovisionedException : MessagingException("Server requires client (re-)provision")

val MEDIATYPE_MESSAGE = MediaType.parse("message/rfc822")
val MEDIATYPE_WBXML = MediaType.parse("application/vnd.ms-sync.wbxml")
const val DEVICE_TYPE = "K9"
const val INITIAL_SYNC_KEY = "0"
const val SUPPORTED_PROTOCOL_EX2007 = "12.0"

const val STATUS_OK = 1

open class EasClient(private val easServerSettings: EasServerSettings,
                     trustManagerFactory: TrustManagerFactory,
                     private val deviceId: String) {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient

    private val authHeader by lazy {
        easServerSettings.run {
            Credentials.basic(username, password)
        }
    }

    private var serverVersion: String? = null

    open var policyKey: String = INITIAL_POLICY_KEY

    init {
        val x509TrustManager = trustManagerFactory.getTrustManagerForDomain(easServerSettings.host, easServerSettings.port)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(x509TrustManager), null)

        okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .sslSocketFactory(sslContext.socketFactory!!, x509TrustManager)
                .build()
    }

    private fun buildUrl(command: String, extra: Pair<String, String>? = null) = easServerSettings.run {
        HttpUrl.Builder().apply {
            scheme(when (connectionSecurity) {
                ConnectionSecurity.NONE -> "http"
                ConnectionSecurity.SSL_TLS_REQUIRED -> "https"
                else -> throw IllegalStateException()
            })
            host(host)
            port(port)
            addPathSegment("Microsoft-Server-ActiveSync")
            addQueryParameter("Cmd", command)
            addQueryParameter("User", username)
            addQueryParameter("DeviceId", deviceId)
            addQueryParameter("DeviceType", DEVICE_TYPE)
            extra?.let { (name, value) ->
                addQueryParameter(name, value)
            }
        }.build()
    }

    private fun post(
            command: String,
            body: RequestBody,
            extra: Pair<String, String>? = null,
            customTimeout: Long? = null
    ): Response {
        initialize()

        val request = Request.Builder()
                .url(buildUrl(command, extra))
                .post(body)
                .header("Authorization", authHeader)
                .header("Connection", "keep-alive")
                .header("User-Agent", "K9/${BuildConfig.VERSION_NAME}")
                .header("MS-ASProtocolVersion", serverVersion!!)
                .header("X-MS-PolicyKey", policyKey)
                .build()

        val client = if (customTimeout != null) {
            okHttpClient.newBuilder()
                    .readTimeout(customTimeout, TimeUnit.SECONDS)
                    .build()
        } else {
            okHttpClient
        }

        return client.newCall(request).execute()
    }

    fun initialize() {
        if (this.serverVersion == null) {
            options()
        }
    }

    fun options() {
        val request = Request.Builder()
                .url(buildUrl("OPTIONS"))
                .method("OPTIONS", null)
                .header("User-Agent", "K9/${BuildConfig.VERSION_NAME}")
                .header("Authorization", authHeader)
                .build()
        val response = okHttpClient.newCall(request).execute()
        ensureSuccessfulResponse(response)

        val versions =
                response.header("ms-asprotocolversions", null) ?: throw MessagingException("versions header missing")
        selectProtocolVersion(versions)
    }

    private fun selectProtocolVersion(versions: String) {
        val versions = versions.split(",")

        serverVersion = when {
            versions.contains(SUPPORTED_PROTOCOL_EX2007) -> SUPPORTED_PROTOCOL_EX2007
            else -> throw MessagingException("server version not supported")
        }
    }

    open fun sendMessage(message: Message) {
        val body = object : RequestBody() {
            override fun contentLength() = message.calculateSize()
            override fun contentType() = MEDIATYPE_MESSAGE

            override fun writeTo(sink: BufferedSink) {
                val msgOut = EOLConvertingOutputStream(sink.outputStream())
                message.writeTo(msgOut)
                msgOut.flush()
            }
        }

        val response = post("SendMail", body, extra = "SaveInSent" to "T")
        ensureSuccessfulResponse(response)
    }

    open fun provision(provisionRequest: Provision): Provision {
        val response = post("Provision", ProvisionDTO(provisionRequest).toWbXmlRequestBody())
        ensureSuccessfulResponse(response)
        return response.body()!!.parseWbXmlResponseBody<ProvisionDTO>().provision
    }

    open fun folderSync(folderSync: FolderSync): FolderSync {
        val response = post("FolderSync", FolderSyncDTO(
                folderSync
        ).toWbXmlRequestBody())
        ensureSuccessfulResponse(response)

        return response.body()!!.parseWbXmlResponseBody<FolderSyncDTO>().folderSync
    }

    open fun sync(sync: Sync): Sync {
        val response = post("Sync", SyncDTO(
                sync
        ).toWbXmlRequestBody())
        ensureSuccessfulResponse(response)
        return response.body()!!.parseWbXmlResponseBody<SyncDTO>().sync
    }

    open fun moveItems(moveItems: MoveItems): MoveItems {
        val response = post("MoveItems", MoveItemsDTO(
                moveItems
        ).toWbXmlRequestBody())
        ensureSuccessfulResponse(response)
        return response.body()!!.parseWbXmlResponseBody<MoveItemsDTO>().moveItems
    }

    open fun ping(ping: Ping, timeout: Long): PingResponse {
        val response = post("Ping", PingDTO(
                ping
        ).toWbXmlRequestBody(), customTimeout = timeout)
        ensureSuccessfulResponse(response)
        return response.body()!!.parseWbXmlResponseBody<PingResponseDTO>().ping
    }

    private fun ensureSuccessfulResponse(response: Response) {
        when (response.code()) {
            in 200..299 -> return
            449 -> throw UnprovisionedException()
            401, 403 -> throw  AuthenticationFailedException("Client could't authenticate or authorize")
            else -> throw IOException("got status ${response.code()}")
        }
    }
}

inline fun <reified T : Any> ResponseBody.parseWbXmlResponseBody() = WbXmlMapper.parse<T>(byteStream())

fun Any.toWbXmlRequestBody() = object : RequestBody() {
    override fun contentType() = MEDIATYPE_WBXML

    override fun writeTo(sink: BufferedSink) {
        WbXmlMapper.serialize(this@toWbXmlRequestBody, sink.outputStream())
    }
}
