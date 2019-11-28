@file:JvmName("MessagingControllerTestExtra")
package com.fsck.k9.preferences

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.imap.ImapStoreUriCreator
import com.fsck.k9.backend.imap.ImapStoreUriDecoder
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriCreator
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriDecoder
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

fun setUpBackendManager() {
    val backendFactory = object : BackendFactory {
        override val transportUriPrefix = "smtp"

        override fun createBackend(account: Account): Backend {
            throw UnsupportedOperationException("not implemented")
        }

        override fun decodeStoreUri(storeUri: String): ServerSettings {
            return ImapStoreUriDecoder.decode(storeUri)
        }

        override fun createStoreUri(serverSettings: ServerSettings): String {
            return ImapStoreUriCreator.create(serverSettings)
        }

        override fun decodeTransportUri(transportUri: String): ServerSettings {
            return SmtpTransportUriDecoder.decodeSmtpUri(transportUri)
        }

        override fun createTransportUri(serverSettings: ServerSettings): String {
            return SmtpTransportUriCreator.createSmtpUri(serverSettings)
        }
    }

    loadKoinModules(module {
        single { BackendManager(mapOf("imap" to backendFactory)) }
    })
}
