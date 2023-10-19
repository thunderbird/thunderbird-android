package app.k9mail.feature.account.server.certificate.domain.entity

data class ServerCertificateProperties(
    val subjectAlternativeNames: List<String>,
    val notValidBefore: String,
    val notValidAfter: String,
    val subject: String,
    val issuer: String,
    val fingerprintSha1: String,
    val fingerprintSha256: String,
    val fingerprintSha512: String,
)
