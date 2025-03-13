package com.fsck.k9.mail.testing.security

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.X509Certificate

object KeyStoreProvider {

    private const val KEYSTORE_PASSWORD = "password"
    private const val KEYSTORE_RESOURCE = "/keystore.jks"
    private const val SERVER_CERTIFICATE_ALIAS = "mockimapserver"

    val keyStore: KeyStore by lazy { loadKeyStore() }
    val password: CharArray by lazy { KEYSTORE_PASSWORD.toCharArray() }
    val serverCertificate: X509Certificate by lazy {
        keyStore.getCertificate(SERVER_CERTIFICATE_ALIAS) as X509Certificate
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("JKS")
        val keyStoreInputStream = KeyStoreProvider::class.java.getResourceAsStream(KEYSTORE_RESOURCE)
        keyStoreInputStream.use { inputStream ->
            keyStore.load(inputStream, KEYSTORE_PASSWORD.toCharArray())
        }

        return keyStore
    }
}
