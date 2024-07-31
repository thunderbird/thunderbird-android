package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AuthenticationType
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal val IncomingServerSettingsContract.State.isPasswordFieldVisible: Boolean
    get() = authenticationType.isPasswordRequired

internal val IncomingServerSettingsContract.State.allowedAuthenticationTypes: ImmutableList<AuthenticationType>
    get() = protocolType.allowedAuthenticationTypes.toImmutableList()

internal val IncomingProtocolType.allowedAuthenticationTypes: List<AuthenticationType>
    get() = when (this) {
        IncomingProtocolType.IMAP -> {
            listOf(
                AuthenticationType.PasswordCleartext,
                AuthenticationType.PasswordEncrypted,
                AuthenticationType.ClientCertificate,
                AuthenticationType.OAuth2,
            )
        }

        IncomingProtocolType.POP3 -> {
            listOf(
                AuthenticationType.PasswordCleartext,
                AuthenticationType.PasswordEncrypted,
                AuthenticationType.ClientCertificate,
            )
        }

        IncomingProtocolType.DDD -> {
            listOf(
                AuthenticationType.PasswordCleartext,
                AuthenticationType.PasswordEncrypted,
                AuthenticationType.ClientCertificate,
            )
        }
    }
