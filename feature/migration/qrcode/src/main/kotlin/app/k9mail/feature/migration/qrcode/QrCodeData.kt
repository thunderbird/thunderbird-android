package app.k9mail.feature.migration.qrcode

internal data class QrCodeData(
    val version: Int,
    val misc: Misc,
    val accounts: List<Account>,
) {
    data class Misc(
        val sequenceNumber: Int,
        val sequenceEnd: Int,
    )

    data class Account(
        val incomingServer: IncomingServer,
        val outgoingServers: List<OutgoingServer>,
    )

    data class IncomingServer(
        val protocol: Int,
        val hostname: String,
        val port: Int,
        val connectionSecurity: Int,
        val authenticationType: Int,
        val username: String,
        val accountName: String?,
        val password: String?,
    )

    data class OutgoingServer(
        val protocol: Int,
        val hostname: String,
        val port: Int,
        val connectionSecurity: Int,
        val authenticationType: Int,
        val username: String,
        val password: String?,
        val identities: List<Identity>,
    )

    data class Identity(
        val emailAddress: String,
        val displayName: String,
    )
}
