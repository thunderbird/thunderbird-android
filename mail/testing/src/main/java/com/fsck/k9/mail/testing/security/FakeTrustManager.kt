package com.fsck.k9.mail.testing.security

import com.fsck.k9.mail.CertificateChainException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@Suppress("CustomX509TrustManager")
class FakeTrustManager : X509TrustManager {
    var shouldThrowException = false

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (shouldThrowException) {
            throw CertificateChainException("Test", chain, Exception("cause"))
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
