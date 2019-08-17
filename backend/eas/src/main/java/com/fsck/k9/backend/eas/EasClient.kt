package com.fsck.k9.backend.eas

import android.os.Build
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ssl.TrustManagerFactory
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.*
import javax.net.ssl.SSLContext


class UnprovisionedException : Exception("Server requires client (re-)provision")
class AuthException : Exception("Client could't authenticate or authorize")

val MEDIATYPE_MESSAGE = MediaType.parse("message/rfc822")
val MEDIATYPE_WBXML = MediaType.parse("application/vnd.ms-sync.wbxml")
const val DEVICE_TYPE = "K9"
const val SUPPORTED_PROTOCOL_EX2007 = "12.0"


class EasClient(private val easServerSettings: EasServerSettings,
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

    var policyKey: String = "0"

    init {
        val x509TrustManager = trustManagerFactory.getTrustManagerForDomain(easServerSettings.host, easServerSettings.port)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(x509TrustManager), null)

        okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .sslSocketFactory(sslContext.socketFactory!!, x509TrustManager)
                .build()
    }

    private fun buildUrl(command: String, extra: String?) = easServerSettings.run {
        HttpUrl.Builder()
                .scheme(when (connectionSecurity) {
                    ConnectionSecurity.NONE -> "http"
                    ConnectionSecurity.SSL_TLS_REQUIRED -> "https"
                    else -> throw IllegalStateException()
                })
                .host(host)
                .port(port)
                .addPathSegment("Microsoft-Server-ActiveSync")
                .addQueryParameter("Cmd", command)
                .addQueryParameter("User", username)
                .addQueryParameter("DeviceId", deviceId)
                .addQueryParameter("DeviceType", DEVICE_TYPE)
                .build()
    }

    private fun post(
            command: String,
            payload: ByteArray,
            extra: String? = null,
            isMessage: Boolean = false
    ): Response {
        initialize()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            println(Base64.getEncoder().encodeToString(payload))
        }

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
                .header("User-Agent", "wio/0.3")
                .header("MS-ASProtocolVersion", serverVersion!!)
                .header("X-MS-PolicyKey", policyKey)
                .build()
        return okHttpClient.newCall(request).execute()
    }

    fun initialize() {
        if (this.serverVersion == null) {
            options()
        }
    }

    fun options() {
        val request = Request.Builder()
                .url(buildUrl("OPTIONS", ""))
                .method("OPTIONS", null)
                .header("Authorization", authHeader)
                .build()
        val response = okHttpClient.newCall(request).execute()
        ensureSuccessfulResponse(response)

        val versions =
                response.header("ms-asprotocolversions", null) ?: throw IOException("versions header missing")
        selectProtocolVersion(versions)
    }

    private fun selectProtocolVersion(versions: String) {
        val versions = versions.split(",")

        serverVersion = when {
            versions.contains(SUPPORTED_PROTOCOL_EX2007) -> SUPPORTED_PROTOCOL_EX2007
            else -> throw IOException("server version not supported")
        }
    }


    fun provision(provisionRequest: Provision): Provision {
        val response = post("Provision", WbXmlMapper.serialize(ProvisionDTO(provisionRequest)))
        ensureSuccessfulResponse(response)
        return WbXmlMapper.parse<ProvisionDTO>(response.body()!!.byteStream())!!.provision
    }

    fun folderSync(folderSync: FolderSync): FolderSync? {
        val response = post(
                "FolderSync", WbXmlMapper.serialize(
                FolderSyncDTO(
                        folderSync
                )
        )
        )
        ensureSuccessfulResponse(response)

        return WbXmlMapper.parse<FolderSyncDTO>(response.body()!!.byteStream())?.folderSync
    }

    fun sync(sync: Sync): Sync? {
        val response = post(
                "Sync", WbXmlMapper.serialize(
                SyncDTO(
                        sync
                )
        )
        )
        ensureSuccessfulResponse(response)
        return WbXmlMapper.parse<SyncDTO>(response.body()!!.byteStream())?.sync
    }

    fun ping(id: String) {
        val ping = PingDTO(
                Ping(
                        1000,
                        PingFolders(
                                PingFolder(
                                        "Email",
                                        id
                                )
                        )
                )
        )


        val response = post("Ping", WbXmlMapper.serialize(ping))
        ensureSuccessfulResponse(response)
        // println(Base64.getEncoder().encodeToString(response.body!!.bytes()))
        println(WbXmlMapper.parse<PingResponseDTO>(response.body()!!.byteStream()))
    }

    private fun ensureSuccessfulResponse(response: Response) {
        when (response.code()) {
            in 200..299 -> return
            449 -> throw UnprovisionedException()
            401, 403 -> throw AuthException()
            else -> throw IOException("got status ${response.code()}")
        }
    }
}
