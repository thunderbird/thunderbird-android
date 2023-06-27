package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.feature.account.setup.domain.entity.toMailConnectionSecurity
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings

// TODO map extras
// TODO map authenticationType
// TODO map clientCertificateAlias
internal fun AccountOutgoingConfigContract.State.toServerSettings(): ServerSettings {
    return ServerSettings(
        type = "smtp",
        host = server.value,
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = AuthType.PLAIN, // TODO replace by actual auth type
        username = username.value,
        password = password.value,
        clientCertificateAlias = null, // TODO replace by actual client certificate alias
    )
}
