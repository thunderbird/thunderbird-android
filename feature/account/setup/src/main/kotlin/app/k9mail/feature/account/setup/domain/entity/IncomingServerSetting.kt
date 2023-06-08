package app.k9mail.feature.account.setup.domain.entity

data class IncomingServerSetting(
    val protocol: IncomingProtocolType,
    val hostname: String,
    val port: Int,
    val connectionSecurity: ConnectionSecurity,
    val authenticationType: AuthenticationType,
    val username: String?,
    val isTrusted: Boolean,
)
