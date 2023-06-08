package app.k9mail.feature.account.setup.domain.entity

data class OutgoingServerSetting(
    val protocol: OutgoingProtocolType,
    val hostname: String,
    val port: Int,
    val connectionSecurity: ConnectionSecurity,
    val authenticationType: AuthenticationType,
    val username: String?,
    val isTrusted: Boolean,
)
