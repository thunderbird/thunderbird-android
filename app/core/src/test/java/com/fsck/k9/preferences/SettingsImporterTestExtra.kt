@file:JvmName("MessagingControllerTestExtra")
package com.fsck.k9.preferences

import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriCreator
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriDecoder
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext.loadKoinModules

fun setUpBackendManager() {
    val backendFactory = object : BackendFactory {
        override val transportUriPrefix = "smtp"

        override fun createBackend(account: Account): Backend {
            throw UnsupportedOperationException("not implemented")
        }

        override fun decodeStoreUri(storeUri: String): ServerSettings {
            return ImapStore.decodeUri(storeUri)
        }

        override fun createStoreUri(serverSettings: ServerSettings): String {
            return ImapStore.createUri(serverSettings)
        }

        override fun decodeTransportUri(transportUri: String): ServerSettings {
            return SmtpTransportUriDecoder.decodeSmtpUri(transportUri)
        }

        override fun createTransportUri(serverSettings: ServerSettings): String {
            return SmtpTransportUriCreator.createSmtpUri(serverSettings)
        }
    }

    loadKoinModules(applicationContext {
        bean { BackendManager(mapOf("imap" to backendFactory)) }
    })
}
