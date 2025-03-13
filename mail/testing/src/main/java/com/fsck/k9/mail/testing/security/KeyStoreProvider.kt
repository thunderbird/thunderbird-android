package com.fsck.k9.mail.testing.security

import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.X509Certificate

class KeyStoreProvider private constructor(@JvmField val keyStore: KeyStore?) {
    val password: CharArray?
        get() = KEYSTORE_PASSWORD.toCharArray()

    val serverCertificate: X509Certificate
        get() {
            try {
                val keyStore: KeyStore = loadKeyStore()
                return keyStore.getCertificate(SERVER_CERTIFICATE_ALIAS) as X509Certificate
            } catch (e: KeyStoreException) {
                throw RuntimeException(e)
            }
        }

    companion object {
        private const val KEYSTORE_PASSWORD = "password"
        private const val KEYSTORE_RESOURCE = "/keystore.jks"
        private const val SERVER_CERTIFICATE_ALIAS = "mockimapserver"

        @JvmStatic
        val instance: KeyStoreProvider
            get() {
                val keyStore: KeyStore = loadKeyStore()
                return KeyStoreProvider(keyStore)
            }

        private fun loadKeyStore(): KeyStore {
            try {
                val keyStore = KeyStore.getInstance("JKS")

                val keyStoreInputStream = KeyStoreProvider::class.java.getResourceAsStream(KEYSTORE_RESOURCE)
                try {
                    keyStore.load(keyStoreInputStream, KEYSTORE_PASSWORD.toCharArray())
                } finally {
                    keyStoreInputStream!!.close()
                }

                return keyStore
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
