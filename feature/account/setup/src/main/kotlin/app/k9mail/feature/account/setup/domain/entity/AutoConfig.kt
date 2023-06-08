package app.k9mail.feature.account.setup.domain.entity

data class AutoConfig(
    val incomingServerSetting: IncomingServerSetting?,
    val outgoingServerSetting: OutgoingServerSetting?,
)
