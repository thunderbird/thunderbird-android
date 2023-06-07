package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity.None
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity.StartTLS
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity.TLS
import kotlinx.collections.immutable.toImmutableList

enum class ConnectionSecurity {
    None,
    StartTLS,
    TLS,
    ;

    companion object {
        val DEFAULT = TLS
        fun all() = values().toList().toImmutableList()
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
