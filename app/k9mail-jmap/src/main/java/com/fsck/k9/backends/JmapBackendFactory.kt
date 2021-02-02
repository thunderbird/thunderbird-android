package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.jmap.JmapBackend
import com.fsck.k9.backend.jmap.JmapConfig
import com.fsck.k9.mailstore.K9BackendStorageFactory

class JmapBackendFactory(
    private val backendStorageFactory: K9BackendStorageFactory,
    private val okHttpClientProvider: OkHttpClientProvider
) : BackendFactory {
    override fun createBackend(account: Account): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val okHttpClient = okHttpClientProvider.getOkHttpClient()

        val serverSettings = account.incomingServerSettings
        val jmapConfig = JmapConfig(
            username = serverSettings.username,
            password = serverSettings.password!!,
            baseUrl = serverSettings.host,
            accountId = serverSettings.extra["accountId"]!!
        )

        return JmapBackend(backendStorage, okHttpClient, jmapConfig)
    }
}
