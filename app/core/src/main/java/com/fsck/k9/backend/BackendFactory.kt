package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings

interface BackendFactory {
    fun createBackend(account: Account): Backend

    fun decodeStoreUri(storeUri: String): ServerSettings
    fun createStoreUri(serverSettings: ServerSettings): String

    val transportUriPrefix: String
    fun decodeTransportUri(transportUri: String): ServerSettings
    fun createTransportUri(serverSettings: ServerSettings): String
}
