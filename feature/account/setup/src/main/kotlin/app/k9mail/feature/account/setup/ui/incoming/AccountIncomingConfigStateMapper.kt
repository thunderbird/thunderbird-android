package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.setup.domain.entity.toAuthType
import app.k9mail.feature.account.setup.domain.entity.toMailConnectionSecurity
import com.fsck.k9.mail.ServerSettings

// TODO map extras
// TODO map clientCertificateAlias
internal fun AccountIncomingConfigContract.State.toServerSettings(): ServerSettings {
    return ServerSettings(
        type = protocolType.defaultName,
        host = server.value,
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = username.value,
        password = if (authenticationType.isPasswordRequired) password.value else null,
        clientCertificateAlias = null, // TODO replace by actual client certificate alias
    )
}
