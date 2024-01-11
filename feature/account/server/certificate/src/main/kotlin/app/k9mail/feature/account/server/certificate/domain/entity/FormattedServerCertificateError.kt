package app.k9mail.feature.account.server.certificate.domain.entity

data class FormattedServerCertificateError(
    val hostname: String,
    val serverCertificateProperties: ServerCertificateProperties,
)
