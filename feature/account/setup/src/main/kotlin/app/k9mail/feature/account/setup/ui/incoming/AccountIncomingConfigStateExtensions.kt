package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.setup.domain.entity.AuthenticationType
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType.ClientCertificate
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType.OAuth2
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType.PasswordCleartext
import app.k9mail.feature.account.setup.domain.entity.AuthenticationType.PasswordEncrypted
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal val AccountIncomingConfigContract.State.isPasswordFieldVisible: Boolean
    get() = authenticationType.isPasswordRequired

internal val AccountIncomingConfigContract.State.allowedAuthenticationTypes: ImmutableList<AuthenticationType>
    get() = protocolType.allowedAuthenticationTypes.toImmutableList()

internal val IncomingProtocolType.allowedAuthenticationTypes: List<AuthenticationType>
    get() = when (this) {
        IncomingProtocolType.IMAP -> {
            listOf(
                PasswordCleartext,
                PasswordEncrypted,
                ClientCertificate,
                OAuth2,
            )
        }

        IncomingProtocolType.POP3 -> {
            listOf(
                PasswordCleartext,
                PasswordEncrypted,
                ClientCertificate,
            )
        }
    }
