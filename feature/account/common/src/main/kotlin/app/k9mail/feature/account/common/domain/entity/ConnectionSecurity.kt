package app.k9mail.feature.account.common.domain.entity

import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity.None
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity.StartTLS
import app.k9mail.feature.account.common.domain.entity.ConnectionSecurity.TLS
import kotlinx.collections.immutable.toImmutableList

enum class ConnectionSecurity {
    None,
    StartTLS,
    TLS,
    ;

    companion object {
        val DEFAULT = TLS
        fun all() = entries.toImmutableList()
    }
}

fun ConnectionSecurity.toMailConnectionSecurity(): MailConnectionSecurity {
    return when (this) {
        None -> MailConnectionSecurity.NONE
        StartTLS -> MailConnectionSecurity.STARTTLS_REQUIRED
        TLS -> MailConnectionSecurity.SSL_TLS_REQUIRED
    }
}

fun MailConnectionSecurity.toConnectionSecurity(): ConnectionSecurity {
    return when (this) {
        MailConnectionSecurity.NONE -> None
        MailConnectionSecurity.STARTTLS_REQUIRED -> StartTLS
        MailConnectionSecurity.SSL_TLS_REQUIRED -> TLS
    }
}

@Suppress("MagicNumber")
fun ConnectionSecurity.toSmtpDefaultPort(): Long {
    return when (this) {
        None -> 587
        StartTLS -> 587
        TLS -> 465
    }
}

@Suppress("MagicNumber")
fun ConnectionSecurity.toImapDefaultPort(): Long {
    return when (this) {
        None -> 143
        StartTLS -> 143
        TLS -> 993
    }
}

@Suppress("MagicNumber")
fun ConnectionSecurity.toPop3DefaultPort(): Long {
    return when (this) {
        None -> 110
        StartTLS -> 110
        TLS -> 995
    }
}
