package com.fsck.k9.mail.ssl

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import net.thunderbird.core.logging.legacy.Log

private const val KEY_STORE_FILE_VERSION = 1
private val PASSWORD = charArrayOf()

class LocalKeyStore(private val directoryProvider: KeyStoreDirectoryProvider) {
    private var keyStoreFile: File? = null
    private val keyStoreDirectory: File by lazy { directoryProvider.getDirectory() }
    private val keyStore: KeyStore? by lazy { initializeKeyStore() }

    @Synchronized
    private fun initializeKeyStore(): KeyStore? {
        upgradeKeyStoreFile()

        val file = getKeyStoreFile(KEY_STORE_FILE_VERSION)
        if (file.length() == 0L) {
            /*
             * The file may be empty (e.g., if it was created with
             * File.createTempFile). We can't pass an empty file to
             * Keystore.load. Instead, we let it be created anew.
             */
            if (file.exists() && !file.delete()) {
                Log.d("Failed to delete empty keystore file: %s", file.absolutePath)
            }
        }

        val fileInputStream = try {
            FileInputStream(file)
        } catch (e: FileNotFoundException) {
            // If the file doesn't exist, that's fine, too
            null
        }

        return try {
            keyStoreFile = file

            KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(fileInputStream, PASSWORD)
            }
        } catch (e: Exception) {
            Log.e(e, "Failed to initialize local key store")

            // Use of the local key store is effectively disabled.
            keyStoreFile = null
            null
        } finally {
            fileInputStream?.close()
        }
    }

    private fun upgradeKeyStoreFile() {
        if (KEY_STORE_FILE_VERSION > 0) {
            // Blow away version "0" because certificate aliases have changed.
            val versionZeroFile = getKeyStoreFile(0)
            if (versionZeroFile.exists() && !versionZeroFile.delete()) {
                Log.d("Failed to delete old key-store file: %s", versionZeroFile.absolutePath)
            }
        }
    }

    @Synchronized
    @Throws(CertificateException::class)
    fun addCertificate(host: String, port: Int, certificate: X509Certificate?) {
        val keyStore = this.keyStore
            ?: throw CertificateException("Certificate not added because key store not initialized")

        try {
            keyStore.setCertificateEntry(getCertKey(host, port), certificate)
        } catch (e: KeyStoreException) {
            throw CertificateException("Failed to add certificate to local key store", e)
        }

        writeCertificateFile()
    }

    private fun writeCertificateFile() {
        val keyStore = requireNotNull(this.keyStore)

        FileOutputStream(keyStoreFile).use { keyStoreStream ->
            try {
                keyStore.store(keyStoreStream, PASSWORD)
            } catch (e: FileNotFoundException) {
                throw CertificateException("Unable to write KeyStore: ${e.message}", e)
            } catch (e: CertificateException) {
                throw CertificateException("Unable to write KeyStore: ${e.message}", e)
            } catch (e: IOException) {
                throw CertificateException("Unable to write KeyStore: ${e.message}", e)
            } catch (e: NoSuchAlgorithmException) {
                throw CertificateException("Unable to write KeyStore: ${e.message}", e)
            } catch (e: KeyStoreException) {
                throw CertificateException("Unable to write KeyStore: ${e.message}", e)
            }
        }
    }

    @Synchronized
    fun isValidCertificate(certificate: Certificate, host: String, port: Int): Boolean {
        val keyStore = this.keyStore ?: return false

        return try {
            val storedCert = keyStore.getCertificate(getCertKey(host, port))
            if (storedCert == null) {
                Log.v("Couldn't find a stored certificate for %s:%d", host, port)
                false
            } else if (storedCert != certificate) {
                Log.v(
                    "Stored certificate for %s:%d doesn't match.\nExpected:\n%s\nActual:\n%s",
                    host,
                    port,
                    storedCert,
                    certificate,
                )
                false
            } else {
                Log.v("Stored certificate for %s:%d matches the server certificate", host, port)
                true
            }
        } catch (e: KeyStoreException) {
            Log.w(e, "Error reading from KeyStore")
            false
        }
    }

    @Synchronized
    fun deleteCertificate(oldHost: String, oldPort: Int) {
        val keyStore = this.keyStore ?: return

        try {
            keyStore.deleteEntry(getCertKey(oldHost, oldPort))
            writeCertificateFile()
        } catch (e: KeyStoreException) {
            // Ignore: most likely there was no cert. found
        } catch (e: CertificateException) {
            Log.e(e, "Error updating the local key store file")
        }
    }

    private fun getKeyStoreFile(version: Int): File {
        return if (version < 1) {
            File(keyStoreDirectory, "KeyStore.bks")
        } else {
            File(keyStoreDirectory, "KeyStore_v$version.bks")
        }
    }

    private fun getCertKey(host: String, port: Int): String {
        return "$host:$port"
    }
}
