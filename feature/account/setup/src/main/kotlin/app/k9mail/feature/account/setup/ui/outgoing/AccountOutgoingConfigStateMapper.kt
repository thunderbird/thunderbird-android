package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import app.k9mail.feature.account.setup.domain.entity.toAuthType
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.State
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import com.fsck.k9.mail.ServerSettings

internal fun AccountSetupState.toOutgoingConfigState(): State {
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

internal fun State.toValidationState(): AccountValidationContract.State {
    return AccountValidationContract.State(
        serverSettings = toServerSettings(),
        // TODO add authorization state
    )
}
