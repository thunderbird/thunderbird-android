package com.fsck.k9

import com.fsck.k9.backend.BackendManager
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.filter.Base64
import org.koin.core.KoinComponent
import org.koin.core.inject

class ServerSettingsSerializer : KoinComponent {
    private val backendManager: BackendManager by inject()

    fun serializeIncoming(serverSettings: ServerSettings): String {
        return Base64.encode(backendManager.createStoreUri(serverSettings))
    }

    fun serializeOutgoing(serverSettings: ServerSettings): String {
        return Base64.encode(backendManager.createTransportUri(serverSettings))
    }

    fun deserializeIncoming(uri: String): ServerSettings {
        return backendManager.decodeStoreUri(Base64.decode(uri))
    }

    fun deserializeOutgoing(uri: String): ServerSettings {
        return backendManager.decodeTransportUri(Base64.decode(uri))
    }
}
