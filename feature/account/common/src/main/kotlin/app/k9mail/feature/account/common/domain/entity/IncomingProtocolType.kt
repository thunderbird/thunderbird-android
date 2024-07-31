package app.k9mail.feature.account.common.domain.entity

import kotlinx.collections.immutable.toImmutableList

enum class IncomingProtocolType(
    val defaultName: String,
    val defaultConnectionSecurity: ConnectionSecurity,
) {
    IMAP("imap", ConnectionSecurity.TLS),
    POP3("pop3", ConnectionSecurity.TLS),
    DDD("ddd", ConnectionSecurity.TLS),
    ;

    companion object {
        val DEFAULT = IMAP

        fun all() = entries.toImmutableList()

        fun fromName(name: String): IncomingProtocolType {
            return entries.find { it.defaultName == name } ?: throw IllegalArgumentException("Unknown protocol: $name")
        }
    }
}

fun IncomingProtocolType.toDefaultPort(connectionSecurity: ConnectionSecurity): Long {
    return when (this) {
        IncomingProtocolType.IMAP -> connectionSecurity.toImapDefaultPort()
        IncomingProtocolType.POP3 -> connectionSecurity.toPop3DefaultPort()
        IncomingProtocolType.DDD -> connectionSecurity.toDddDefaultPort()
    }
}
