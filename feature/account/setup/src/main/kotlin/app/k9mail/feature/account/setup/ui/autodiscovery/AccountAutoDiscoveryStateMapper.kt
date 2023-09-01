package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toIncomingProtocolType
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.toServerSettings
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract

internal fun AccountAutoDiscoveryContract.State.toAccountState(): AccountState {
    return AccountState(
        emailAddress = emailAddress.value,
        incomingServerSettings = autoDiscoverySettings?.incomingServerSettings?.toServerSettings(password.value),
        outgoingServerSettings = autoDiscoverySettings?.outgoingServerSettings?.toServerSettings(password.value),
        authorizationState = authorizationState,
        options = null,
    )
}

internal fun AccountAutoDiscoveryContract.State.toIncomingConfigState(): AccountIncomingConfigContract.State {
    val incomingSettings = autoDiscoverySettings?.incomingServerSettings as? ImapServerSettings?
    return if (incomingSettings == null) {
        AccountIncomingConfigContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
        )
    } else {
        AccountIncomingConfigContract.State(
            protocolType = incomingSettings.toIncomingProtocolType(),
            server = StringInputField(value = incomingSettings.hostname.value),
            security = incomingSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = incomingSettings.port.value.toLong()),
            authenticationType = incomingSettings.authenticationTypes.first().toAuthenticationType(),
            username = StringInputField(value = incomingSettings.username),
            password = StringInputField(value = password.value),
        )
    }
}

internal fun AccountAutoDiscoveryContract.State.toOutgoingConfigState(): AccountOutgoingConfigContract.State {
    val outgoingSettings = autoDiscoverySettings?.outgoingServerSettings as? SmtpServerSettings?
    return if (outgoingSettings == null) {
        AccountOutgoingConfigContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
        )
    } else {
        AccountOutgoingConfigContract.State(
            server = StringInputField(value = outgoingSettings.hostname.value),
            security = outgoingSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = outgoingSettings.port.value.toLong()),
            authenticationType = outgoingSettings.authenticationTypes.first().toAuthenticationType(),
            username = StringInputField(value = outgoingSettings.username),
            password = StringInputField(value = password.value),
        )
    }
}

internal fun AccountAutoDiscoveryContract.State.toOptionsState(): AccountOptionsContract.State {
    return AccountOptionsContract.State(
        accountName = StringInputField(value = emailAddress.value),
    )
}
