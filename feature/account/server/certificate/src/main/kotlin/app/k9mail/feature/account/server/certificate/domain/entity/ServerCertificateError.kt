package app.k9mail.feature.account.server.certificate.domain.entity

import java.security.cert.X509Certificate

data class ServerCertificateError(
    val hostname: String,
    val port: Int,
    val certificateChain: List<X509Certificate>,
)
