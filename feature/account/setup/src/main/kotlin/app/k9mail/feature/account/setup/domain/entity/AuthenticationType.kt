package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

enum class AuthenticationType(
    val isPasswordRequired: Boolean,
) {
    None(
        isPasswordRequired = false,
    ),
    PasswordCleartext(
        isPasswordRequired = true,
    ),
    PasswordEncrypted(
        isPasswordRequired = true,
    ),
    ClientCertificate(
        isPasswordRequired = false,
    ),
    OAuth2(
        isPasswordRequired = false,
    ),
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
