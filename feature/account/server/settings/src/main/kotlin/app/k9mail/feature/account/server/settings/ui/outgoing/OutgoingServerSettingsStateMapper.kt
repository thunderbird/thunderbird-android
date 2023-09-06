package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.toAuthType
import app.k9mail.feature.account.common.domain.entity.toAuthenticationType
import app.k9mail.feature.account.common.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import com.fsck.k9.mail.ServerSettings

fun AccountState.toOutgoingConfigState(): State {
    val outgoingServerSettings = outgoingServerSettings
    return if (outgoingServerSettings == null) {
        State(
            username = StringInputField(value = emailAddress ?: ""),
        )
    } else {
        State(
            server = StringInputField(value = outgoingServerSettings.host ?: ""),
            security = outgoingServerSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = outgoingServerSettings.port.toLong()),
            authenticationType = outgoingServerSettings.authenticationType.toAuthenticationType(),
            username = StringInputField(value = outgoingServerSettings.username),
            password = StringInputField(value = outgoingServerSettings.password ?: ""),
        )
    }
}

internal fun State.toServerSettings(): ServerSettings {
    return ServerSettings(
        type = "smtp",
        host = server.value,
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = if (authenticationType.isUsernameRequired) username.value else "",
        password = if (authenticationType.isPasswordRequired) password.value else null,
        clientCertificateAlias = clientCertificateAlias,
    )
}
