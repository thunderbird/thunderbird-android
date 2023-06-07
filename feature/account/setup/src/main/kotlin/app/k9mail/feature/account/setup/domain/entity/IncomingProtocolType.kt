package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

enum class IncomingProtocolType {
    IMAP,
    POP3,
    ;

    companion object {
        val DEFAULT = IMAP

        fun all() = values().toList().toImmutableList()
    }
}

@Suppress("SameReturnValue")
fun IncomingProtocolType.toDefaultSecurity(): ConnectionSecurity {
    return when (this) {
        IncomingProtocolType.IMAP -> ConnectionSecurity.TLS
        IncomingProtocolType.POP3 -> ConnectionSecurity.TLS
    }
}

fun IncomingProtocolType.toDefaultPort(connectionSecurity: ConnectionSecurity): Long {
    return when (this) {
        IncomingProtocolType.IMAP -> connectionSecurity.toImapDefaultPort()
        IncomingProtocolType.POP3 -> connectionSecurity.toPop3DefaultPort()
    }
}
