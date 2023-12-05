package com.fsck.k9.mail.ssl

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.mail.CertificateChainException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLException
import kotlin.test.Test

class CertificateChainExtractorTest {
    @Test
    fun `input is a CertificateChainException`() {
        val throwable = CertificateChainException(
            "irrelevant",
            arrayOf(CERTIFICATE),
            null,
        )

        val result = CertificateChainExtractor.extract(throwable)

        assertThat(result).isNotNull().containsExactly(CERTIFICATE)
    }

    @Test
    fun `SSLException containing CertificateChainException as direct child`() {
        val throwable = SSLException(
            CertificateChainException(
                "irrelevant",
                arrayOf(CERTIFICATE),
                null,
            ),
        )

        val result = CertificateChainExtractor.extract(throwable)

        assertThat(result).isNotNull().containsExactly(CERTIFICATE)
    }

    @Test
    fun `SSLException containing CertificateChainException as indirect child`() {
        val throwable = SSLException(
            CertificateException(
                CertificateChainException(
                    "irrelevant",
                    arrayOf(CERTIFICATE),
                    null,
                ),
            ),
        )

        val result = CertificateChainExtractor.extract(throwable)

        assertThat(result).isNotNull().containsExactly(CERTIFICATE)
    }

    @Test
    fun `SSLException without a cause`() {
        val throwable = SSLException("irrelevant")

        val result = CertificateChainExtractor.extract(throwable)

        assertThat(result).isNull()
    }

    @Test
    fun `SSLException with multiple non-CertificateChainException children`() {
        val throwable = SSLException(
            IllegalStateException(
                NumberFormatException(),
            ),
        )

        val result = CertificateChainExtractor.extract(throwable)

        assertThat(result).isNull()
    }

    companion object {
        private val CERTIFICATE = readCertificate("mail.domain.example")

        @Suppress("SameParameterValue")
        private fun readCertificate(name: String): X509Certificate {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            this::class.java.getResourceAsStream("/certificates/$name.pem")!!.let { inputStream ->
                return certificateFactory.generateCertificate(inputStream) as X509Certificate
            }
        }
    }
}
