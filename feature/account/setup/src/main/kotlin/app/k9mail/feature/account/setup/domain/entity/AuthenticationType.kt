package app.k9mail.feature.account.setup.domain.entity

import com.fsck.k9.mail.AuthType
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

        fun outgoing() = all()
    }
}

fun AuthenticationType.toAuthType(): AuthType {
    return when (this) {
        AuthenticationType.None -> AuthType.PLAIN
        AuthenticationType.PasswordCleartext -> AuthType.PLAIN
        AuthenticationType.PasswordEncrypted -> AuthType.CRAM_MD5
        AuthenticationType.ClientCertificate -> AuthType.EXTERNAL
        AuthenticationType.OAuth2 -> AuthType.XOAUTH2
    }
}
