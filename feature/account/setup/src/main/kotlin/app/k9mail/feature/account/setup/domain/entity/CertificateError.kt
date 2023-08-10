package app.k9mail.feature.account.setup.domain.entity

import java.security.cert.X509Certificate

data class CertificateError(
    val hostname: String,
    val port: Int,
    val certificateChain: List<X509Certificate>,
)
