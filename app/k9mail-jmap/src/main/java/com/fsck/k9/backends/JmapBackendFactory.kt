package com.fsck.k9.backends

import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.jmap.JmapBackend
import com.fsck.k9.backend.jmap.JmapConfig
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.K9BackendStorageFactory

class JmapBackendFactory(
    private val backendStorageFactory: K9BackendStorageFactory,
    private val okHttpClientProvider: OkHttpClientProvider
) : BackendFactory {
    override val transportUriPrefix = "jmap"

    override fun createBackend(account: Account): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val okHttpClient = okHttpClientProvider.getOkHttpClient()

        val serverSettings = decodeStoreUri(account.storeUri)
        val jmapConfig = JmapConfig(
            username = serverSettings.username,
            password = serverSettings.password,
            baseUrl = serverSettings.host,
            accountId = serverSettings.extra["accountId"]!!
        )

        return JmapBackend(backendStorage, okHttpClient, jmapConfig)
    }

    override fun decodeStoreUri(storeUri: String): ServerSettings {
        val uri = Uri.parse(storeUri)
        val username = uri.getQueryParameter("username")
        val password = uri.getQueryParameter("password")
        val baseUrl = uri.getQueryParameter("baseUrl")
        val accountId = uri.getQueryParameter("accountId")

        val extra = mapOf(
            "accountId" to accountId
        )

        return ServerSettings("jmap", baseUrl, 433, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, username, password, null, extra)
    }

    override fun createStoreUri(serverSettings: ServerSettings): String {
        val username = serverSettings.username
        val password = serverSettings.password
        val baseUrl = serverSettings.host
        val accountId = serverSettings.extra["accountId"]

        return Uri.Builder()
            .scheme("jmap")
            .authority("unused")
            .appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .apply {
                if (baseUrl != null) {
                    appendQueryParameter("baseUrl", baseUrl)
                }
            }
            .appendQueryParameter("accountId", accountId)
            .build()
            .toString()
    }

    override fun decodeTransportUri(transportUri: String): ServerSettings {
        return decodeStoreUri(transportUri)
    }

    override fun createTransportUri(serverSettings: ServerSettings): String {
        return createStoreUri(serverSettings)
    }
}
