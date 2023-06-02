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
