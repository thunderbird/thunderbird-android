package net.thunderbird.core.common.net

import assertk.assertFailure
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.net.tls.CertificateBlocklist
import net.thunderbird.core.common.net.tls.CertificateBlocklist.Companion.CVE_2026_111943
import org.junit.Test

class CertificateBlocklistTest {

    @Test
    fun `safe certificate hash is not blocked`() = runTest {
        TODO()
    }

    @Test
    fun `known unsafe certificate hash is blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist()
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val unsafeByteStream = CVE_2026_111943.random().toByteArray().inputStream()
        val unsafeCert = certificateFactory.generateCertificate(unsafeByteStream) as X509Certificate
        assertFailure { certificateBlockList.isBlockedSpkiSha256(arrayOf(unsafeCert)) }
    }

    @Test
    fun `uppercase unsafe certificate hash is blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist()
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val unsafeByteStream = CVE_2026_111943.random().toAsciiUppercase().toByteArray().inputStream()
        val unsafeCert = certificateFactory.generateCertificate(unsafeByteStream) as X509Certificate
        assertFailure { certificateBlockList.isBlockedSpkiSha256(arrayOf(unsafeCert)) }
    }
}
