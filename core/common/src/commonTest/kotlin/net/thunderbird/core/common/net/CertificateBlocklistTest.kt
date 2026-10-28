package net.thunderbird.core.common.net

import assertk.assertAll
import assertk.assertFailure
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.io.encoding.Base64
import kotlin.jvm.java
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.net.tls.CertificateBlocklist
import net.thunderbird.core.common.net.tls.CertificateBlocklist.Companion.CVE_2026_111943
import net.thunderbird.core.common.net.tls.CompromisedCertificateException
import okhttp3.CertificatePinner.Companion.sha256Hash
import okio.ByteString.Companion.decodeHex
import org.junit.Test

class CertificateBlocklistTest {

    @Test
    fun `safe certificate hash is not blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist()
        val hostCert = getGoodHostCertificate()
        assertAll { certificateBlockList.isBlockedSpkiSha256(arrayOf(hostCert)) }
    }

    @Test
    fun `known unsafe certificate hash is blocked`() = runTest {
        TODO()
//        val certificateBlockList = CertificateBlocklist()
//        val certificateFactory = CertificateFactory.getInstance("X.509")
//        val unsafeByteArray = CVE_2026_111943.random().hex().toByteArray()
//        // TODO: generate a certificate from the byte array
//        val unsafeByteStream = generateCertificateInputStream(unsafeByteArray)
//        val unsafeCert = certificateFactory.generateCertificate(unsafeByteStream) as X509Certificate
//        assertFailsWith(CompromisedCertificateException::class) {
//            certificateBlockList.isBlockedSpkiSha256(arrayOf(unsafeCert))
//        }
    }

    @Test
    fun `uppercase unsafe certificate hash is blocked`() = runTest {
        TODO()
    }

    private fun getGoodHostCertificate(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        CertificateBlocklist::class.java.getResourceAsStream("/certificates/goodHost.pem")!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }
}
