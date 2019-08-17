package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.eas.EasBackend
import com.fsck.k9.backend.eas.EasServerSettings
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.fsck.k9.mailstore.K9BackendStorageFactory

class EasBackendFactory(
        private val backendStorageFactory: K9BackendStorageFactory,
        private val trustManagerFactory: TrustManagerFactory
) : BackendFactory {
    override val transportUriPrefix = "eas"

    override fun createBackend(account: Account): Backend {
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val serverSettings = EasServerSettings.decode(account.storeUri)
        val deviceId = "23413"

        return EasBackend(backendStorage, trustManagerFactory, serverSettings, deviceId)
    }

    override fun decodeStoreUri(storeUri: String): ServerSettings {
        return EasServerSettings.decode(storeUri)
    }

    override fun createStoreUri(serverSettings: ServerSettings): String {
        return EasServerSettings.encode(serverSettings)
    }

    override fun decodeTransportUri(transportUri: String): ServerSettings {
        return EasServerSettings.decode(transportUri)
    }

    override fun createTransportUri(serverSettings: ServerSettings): String {
        return EasServerSettings.encode(serverSettings)
    }
}
