package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

enum class AuthenticationType {
    PLAIN,
    CRAM_MD5,
    EXTERNAL,
    OAUTH2,
    ;

    companion object {
        val DEFAULT = PLAIN
        fun all() = values().toList().toImmutableList()
    }
}
