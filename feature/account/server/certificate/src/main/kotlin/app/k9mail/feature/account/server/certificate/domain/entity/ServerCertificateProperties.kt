package app.k9mail.feature.account.server.certificate.domain.entity

import okio.ByteString

data class ServerCertificateProperties(
    val subjectAlternativeNames: List<String>,
    val notValidBefore: String,
    val notValidAfter: String,
    val subject: String,
    val issuer: String,
    val fingerprintSha1: ByteString,
    val fingerprintSha256: ByteString,
    val fingerprintSha512: ByteString,
)
