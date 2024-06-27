package app.k9mail.feature.account.common.domain.entity

import com.fsck.k9.mail.AuthType
import kotlinx.collections.immutable.toImmutableList

enum class AuthenticationType(
    val isUsernameRequired: Boolean,
    val isPasswordRequired: Boolean,
) {
    None(
        isUsernameRequired = false,
        isPasswordRequired = false,
    ),
    PasswordCleartext(
        isUsernameRequired = true,
        isPasswordRequired = true,
    ),
    PasswordEncrypted(
        isUsernameRequired = true,
        isPasswordRequired = true,
    ),
    ClientCertificate(
        isUsernameRequired = true,
        isPasswordRequired = false,
    ),
    OAuth2(
        isUsernameRequired = true,
        isPasswordRequired = false,
    ),
    ;

    companion object {
        val DEFAULT = PasswordCleartext
        fun all() = entries.toImmutableList()

        fun outgoing() = all()
    }
}

fun AuthenticationType.toAuthType(): AuthType {
    return when (this) {
        AuthenticationType.None -> AuthType.NONE
        AuthenticationType.PasswordCleartext -> AuthType.PLAIN
        AuthenticationType.PasswordEncrypted -> AuthType.CRAM_MD5
        AuthenticationType.ClientCertificate -> AuthType.EXTERNAL
        AuthenticationType.OAuth2 -> AuthType.XOAUTH2
    }
}

fun AuthType.toAuthenticationType(): AuthenticationType {
    return when (this) {
        AuthType.PLAIN -> AuthenticationType.PasswordCleartext
        AuthType.CRAM_MD5 -> AuthenticationType.PasswordEncrypted
        AuthType.EXTERNAL -> AuthenticationType.ClientCertificate
        AuthType.XOAUTH2 -> AuthenticationType.OAuth2
        AuthType.NONE -> AuthenticationType.None
    }
}
