package com.fsck.k9.mail.ssl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import java.nio.file.Files
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.junit.After
import org.junit.Test

class LocalKeyStoreTest {
    private val host = "mail.domain.example"
    private val port = 587

    private val tempDirectory = Files.createTempDirectory("KeyStore").toFile()
    private val context = mock<Context> {
        on { getDir("KeyStore", Context.MODE_PRIVATE) } doReturn tempDirectory
    }
    private val localKeyStore = LocalKeyStore.createInstance(context)

    @After
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `empty LocalKeyStore should not consider certificate valid`() {
        val certificate = readCertificate(host)

        val isCertificateValid = localKeyStore.isValidCertificate(certificate, host, port)

        assertThat(isCertificateValid).isFalse()
    }

    @Test
    fun `stored certificate should be considered valid`() {
        val certificate = readCertificate(host)
        localKeyStore.addCertificate(host, port, certificate)

        val isCertificateValid = localKeyStore.isValidCertificate(certificate, host, port)

        assertThat(isCertificateValid).isTrue()
    }

    @Test
    fun `host names must match`() {
        val certificate = readCertificate(host)
        localKeyStore.addCertificate(host, port, certificate)

        val isCertificateValid = localKeyStore.isValidCertificate(certificate, "evil.domain.example", port)

        assertThat(isCertificateValid).isFalse()
    }

    @Test
    fun `port numbers must match`() {
        val certificate = readCertificate(host)
        localKeyStore.addCertificate(host, port, certificate)

        val isCertificateValid = localKeyStore.isValidCertificate(certificate, host, 123)

        assertThat(isCertificateValid).isFalse()
    }

    @Test
    fun `different certificate should not be considered valid`() {
        val certificate = readCertificate(host)
        localKeyStore.addCertificate(host, port, certificate)
        val anotherCertificate = readCertificate("mail.another-domain.example")

        val isCertificateValid = localKeyStore.isValidCertificate(anotherCertificate, host, port)

        assertThat(isCertificateValid).isFalse()
    }

    @Test
    fun `deleted certificate shouldn't be considered valid`() {
        val certificate = readCertificate(host)
        localKeyStore.addCertificate(host, port, certificate)
        localKeyStore.deleteCertificate(host, port)

        val isCertificateValid = localKeyStore.isValidCertificate(certificate, host, port)

        assertThat(isCertificateValid).isFalse()
    }

    private fun readCertificate(name: String): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        LocalKeyStoreTest::class.java.getResourceAsStream("/certificates/$name.pem")!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }
}
