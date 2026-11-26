package net.thunderbird.core.common.net

import assertk.assertAll
import assertk.assertFailure
import assertk.assertions.isInstanceOf
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.jvm.java
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.net.tls.CertificateBlocklist
import net.thunderbird.core.common.net.tls.CompromisedCertificateException
import org.junit.Test

class CertificateBlocklistTest {

    @Test
    fun `safe certificate hash is not blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist
        val hostCert = getGoodHostCertificate()
        assertAll { certificateBlockList.checkCertificateBlockList(arrayOf(hostCert)) }
    }

    @Test
    fun `known unsafe certificate hash is blocked`() = runTest {
        val certificateBlockList = CertificateBlocklist
        val badCert = getBadHostCertificate()
        assertFailure {
            certificateBlockList.checkCertificateBlockList(arrayOf(badCert))
        }.isInstanceOf<CompromisedCertificateException>()
    }

    @Test
    fun `known unsafe certificate hash is blocked in middle of chain`() = runTest {
        val certificateBlockList = CertificateBlocklist
        val goodCert = getGoodHostCertificate()
        val badCert = getBadHostCertificate()
        assertFailure {
            certificateBlockList.checkCertificateBlockList(arrayOf(goodCert, badCert, goodCert))
        }.isInstanceOf<CompromisedCertificateException>()
    }

    private fun getGoodHostCertificate(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        CertificateBlocklistTest::class.java.getResourceAsStream("/certificates/goodHost.pem")!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }

    private fun getBadHostCertificate(): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        CertificateBlocklistTest::class.java.getResourceAsStream(
            "/certificates/cve-2026-11943-bad-cert.pem",
        )!!.let { inputStream ->
            return certificateFactory.generateCertificate(inputStream) as X509Certificate
        }
    }
}
