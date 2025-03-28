package com.fsck.k9

import app.k9mail.legacy.account.Account
import com.fsck.k9.mail.ssl.LocalKeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import net.thunderbird.core.mail.mailserver.MailServerDirection

class LocalKeyStoreManager(
    private val localKeyStore: LocalKeyStore,
) {
    /**
     * Add a new certificate for the incoming or outgoing server to the local key store.
     */
    @Throws(CertificateException::class)
    fun addCertificate(account: Account, direction: MailServerDirection, certificate: X509Certificate) {
        val serverSettings = if (direction === MailServerDirection.INCOMING) {
            account.incomingServerSettings
        } else {
            account.outgoingServerSettings
        }
        localKeyStore.addCertificate(serverSettings.host!!, serverSettings.port, certificate)
    }

    /**
     * Examine the existing settings for an account.  If the old host/port is different from the
     * new host/port, then try and delete any (possibly non-existent) certificate stored for the
     * old host/port.
     */
    fun deleteCertificate(account: Account, newHost: String, newPort: Int, direction: MailServerDirection) {
        val serverSettings = if (direction === MailServerDirection.INCOMING) {
            account.incomingServerSettings
        } else {
            account.outgoingServerSettings
        }
        val oldHost = serverSettings.host!!
        val oldPort = serverSettings.port
        if (oldPort == -1) {
            // This occurs when a new account is created
            return
        }
        if (newHost != oldHost || newPort != oldPort) {
            localKeyStore.deleteCertificate(oldHost, oldPort)
        }
    }

    /**
     * Examine the settings for the account and attempt to delete (possibly non-existent)
     * certificates for the incoming and outgoing servers.
     */
    fun deleteCertificates(account: Account) {
        account.incomingServerSettings.let { serverSettings ->
            localKeyStore.deleteCertificate(serverSettings.host!!, serverSettings.port)
        }

        account.outgoingServerSettings.let { serverSettings ->
            localKeyStore.deleteCertificate(serverSettings.host!!, serverSettings.port)
        }
    }
}
