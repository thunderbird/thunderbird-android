package net.thunderbird.feature.mail.message.composer.smime

import java.security.PrivateKey
import java.security.cert.X509Certificate

data class SmimeSigningCredentials(
    val alias: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
)

data class SmimeCertificateInfo(
    val alias: String,
    val subjectDisplayName: String,
    val issuerDisplayName: String?,
    val isExpired: Boolean,
)

interface SmimeKeyChainProvider {
    @Throws(SmimeSigningException::class)
    fun getSigningCredentials(alias: String): SmimeSigningCredentials

    fun getCertificateInfo(alias: String): SmimeCertificateInfo?
}
