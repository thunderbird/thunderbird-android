package com.fsck.k9.backend.eas

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.TrustManagerFactory
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
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
                     private val trustManagerFactory: TrustManagerFactory,
                     private val deviceId: String) {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
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
            payload: ByteArray,
            extra: Pair<String, String>? = null,
            isMessage: Boolean = false,
            customTimeout: Long? = null
    ): Response {
        initialize()

        val body = RequestBody.create(
                if (isMessage) {
                    MEDIATYPE_MESSAGE
                } else {
                    MEDIATYPE_WBXML
                }, payload
        )

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

    open fun sendMessage(data: ByteArray) {
        val response = post("SendMail", data, "SaveInSent" to "T", isMessage = true)
        ensureSuccessfulResponse(response)
    }

    open fun provision(provisionRequest: Provision): Provision {
        val response = post("Provision", WbXmlMapper.serialize(ProvisionDTO(provisionRequest)))
        ensureSuccessfulResponse(response)
        return WbXmlMapper.parse<ProvisionDTO>(response.body()!!.byteStream()).provision
    }

    open fun folderSync(folderSync: FolderSync): FolderSync {
        val response = post("FolderSync", WbXmlMapper.serialize(
                FolderSyncDTO(
                        folderSync
                )
        ))
        ensureSuccessfulResponse(response)

        return WbXmlMapper.parse<FolderSyncDTO>(response.body()!!.byteStream()).folderSync
    }

    open fun sync(sync: Sync): Sync {
        val response = post("Sync", WbXmlMapper.serialize(
                SyncDTO(
                        sync
                )
        ))
        ensureSuccessfulResponse(response)
        return WbXmlMapper.parse<SyncDTO>(response.body()!!.byteStream()).sync
    }

    fun ping(ping: Ping, timeout: Long): PingResponse {
        val response = post("Ping", WbXmlMapper.serialize(PingDTO(
                ping
        )), customTimeout = timeout)
        ensureSuccessfulResponse(response)
        return WbXmlMapper.parse<PingResponseDTO>(response.body()!!.byteStream()).ping
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
