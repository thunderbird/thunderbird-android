package app.k9mail.feature.account.common.domain.entity

import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.core.common.mail.Protocols

private const val NATIVE_API_DEFAULT_PORT = 443L

enum class IncomingProtocolType(
    val defaultName: String,
    val defaultConnectionSecurity: ConnectionSecurity,
) {
    IMAP("imap", ConnectionSecurity.TLS),
    POP3("pop3", ConnectionSecurity.TLS),
    GMAIL_API(Protocols.GMAIL_API, ConnectionSecurity.TLS),
    GRAPH(Protocols.GRAPH, ConnectionSecurity.TLS),
    ZOHO_MAIL_API(Protocols.ZOHO_MAIL_API, ConnectionSecurity.TLS),
    ;

    companion object {
        val DEFAULT = IMAP

        fun all() = entries.toImmutableList()

        fun fromName(name: String): IncomingProtocolType {
            return fromNameOrNull(name) ?: throw IllegalArgumentException("Unknown protocol: $name")
        }

        fun fromNameOrNull(name: String): IncomingProtocolType? {
            return entries.find { it.defaultName == name }
                ?: GRAPH.takeIf { Protocols.isGraphProtocol(name) }
        }
    }
}

fun IncomingProtocolType.toDefaultPort(connectionSecurity: ConnectionSecurity): Long {
    return when (this) {
        IncomingProtocolType.IMAP -> connectionSecurity.toImapDefaultPort()

        IncomingProtocolType.POP3 -> connectionSecurity.toPop3DefaultPort()

        IncomingProtocolType.GMAIL_API,
        IncomingProtocolType.GRAPH,
        IncomingProtocolType.ZOHO_MAIL_API,
        -> NATIVE_API_DEFAULT_PORT
    }
}

fun IncomingProtocolType.isNativeRestApi(): Boolean = when (this) {
    IncomingProtocolType.GMAIL_API,
    IncomingProtocolType.GRAPH,
    IncomingProtocolType.ZOHO_MAIL_API,
    -> true

    IncomingProtocolType.IMAP,
    IncomingProtocolType.POP3,
    -> false
}
