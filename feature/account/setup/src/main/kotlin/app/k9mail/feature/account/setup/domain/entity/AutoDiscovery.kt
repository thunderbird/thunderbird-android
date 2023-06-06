package app.k9mail.feature.account.setup.domain.entity

data class AutoDiscovery(
    val incomingServerSetting: IncomingServerSetting,
    val outgoingServerSetting: OutgoingServerSetting,
    val isTrusted: Boolean,
)
