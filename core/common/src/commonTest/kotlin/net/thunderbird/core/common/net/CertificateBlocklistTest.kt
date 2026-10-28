package net.thunderbird.core.common.net

import assertk.assertAll
import assertk.assertFailure
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.io.encoding.Base64
import kotlin.jvm.java
import kotlin.test.Ignore
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

    @Ignore("TODO: Create unsafe hash in resources/certificates/badHost.pem")
    @Test
    fun `known unsafe certificate hash is blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist()
        val badCert = getBadHostCertificate()
        assertFailsWith(CompromisedCertificateException::class) {
            certificateBlockList.isBlockedSpkiSha256(arrayOf(badCert))
        }
    }

    private fun getGoodHostCertificate(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        CertificateBlocklistTest::class.java.getResourceAsStream("/certificates/goodHost.pem")!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }

    private fun getBadHostCertificate(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        CertificateBlocklistTest::class.java.getResourceAsStream("/certificates/badHost.pem")!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }
}
