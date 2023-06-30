package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

enum class AuthenticationType {
    None,
    PasswordCleartext,
    PasswordEncrypted,
    ClientCertificate,
    OAuth2,
    ;

    companion object {
        val DEFAULT = PasswordCleartext
        fun all() = values().toList().toImmutableList()

        fun incoming() = listOf(
            PasswordCleartext,
            PasswordEncrypted,
            ClientCertificate,
            OAuth2,
        ).toImmutableList()

        fun outgoing() = all()
    }
}
