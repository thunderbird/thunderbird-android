package com.fsck.k9

import android.net.Uri
import com.fsck.k9.mail.MailServerDirection
import com.fsck.k9.mail.ssl.LocalKeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class LocalKeyStoreManager(
        val localKeyStore: LocalKeyStore
) {
    /**
     * Add a new certificate for the incoming or outgoing server to the local key store.
     */
    @Throws(CertificateException::class)
    fun addCertificate(account: Account, direction: MailServerDirection, certificate: X509Certificate) {
        val uri: Uri
        if (direction === MailServerDirection.INCOMING) {
            uri = Uri.parse(account.storeUri)
        } else {
            uri = Uri.parse(account.transportUri)
        }
        localKeyStore.addCertificate(uri.host, uri.port, certificate)
    }

    /**
     * Examine the existing settings for an account.  If the old host/port is different from the
     * new host/port, then try and delete any (possibly non-existent) certificate stored for the
     * old host/port.
     */
    fun deleteCertificate(account: Account, newHost: String, newPort: Int, direction: MailServerDirection) {
        val uri: Uri
        if (direction === MailServerDirection.INCOMING) {
            uri = Uri.parse(account.storeUri)
        } else {
            uri = Uri.parse(account.transportUri)
        }
        val oldHost = uri.host
        val oldPort = uri.port
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
        val storeUri = account.storeUri
        if (storeUri != null) {
            val uri = Uri.parse(storeUri)
            localKeyStore.deleteCertificate(uri.host, uri.port)
        }
        val transportUri = account.transportUri
        if (transportUri != null) {
            val uri = Uri.parse(transportUri)
            localKeyStore.deleteCertificate(uri.host, uri.port)
        }
    }
}