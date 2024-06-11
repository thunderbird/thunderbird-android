package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.input.NumberInputField
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toIncomingProtocolType
import app.k9mail.feature.account.setup.domain.toServerSettings
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract

internal fun AccountAutoDiscoveryContract.State.toAccountState(): AccountState {
    return AccountState(
        emailAddress = emailAddress.value,
        incomingServerSettings = autoDiscoverySettings?.incomingServerSettings?.toServerSettings(password.value),
        outgoingServerSettings = autoDiscoverySettings?.outgoingServerSettings?.toServerSettings(password.value),
        authorizationState = authorizationState,
        displayOptions = null,
        syncOptions = null,
    )
}

internal fun AccountAutoDiscoveryContract.State.toIncomingConfigState(): IncomingServerSettingsContract.State {
    val incomingSettings = autoDiscoverySettings?.incomingServerSettings as? ImapServerSettings?
    return if (incomingSettings == null) {
        IncomingServerSettingsContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
        )
    } else {
        IncomingServerSettingsContract.State(
            protocolType = incomingSettings.toIncomingProtocolType(),
            server = StringInputField(value = incomingSettings.hostname.value),
            security = incomingSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = incomingSettings.port.value.toLong()),
            authenticationType = incomingSettings.authenticationTypes.first().toAuthenticationType(),
            username = StringInputField(value = incomingSettings.username),
            password = StringInputField(value = password.value),
            imapAutodetectNamespaceEnabled = true,
            imapPrefix = StringInputField(value = ""),
            imapUseCompression = true,
            imapSendClientInfo = true,
        )
    }
}

internal fun AccountAutoDiscoveryContract.State.toOutgoingConfigState(): OutgoingServerSettingsContract.State {
    val outgoingSettings = autoDiscoverySettings?.outgoingServerSettings as? SmtpServerSettings?
    return if (outgoingSettings == null) {
        OutgoingServerSettingsContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
        )
    } else {
        OutgoingServerSettingsContract.State(
            server = StringInputField(value = outgoingSettings.hostname.value),
            security = outgoingSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = outgoingSettings.port.value.toLong()),
            authenticationType = outgoingSettings.authenticationTypes.first().toAuthenticationType(),
            username = StringInputField(value = outgoingSettings.username),
            password = StringInputField(value = password.value),
        )
    }
}

internal fun AccountAutoDiscoveryContract.State.toOptionsState(): DisplayOptionsContract.State {
    return DisplayOptionsContract.State(
        accountName = StringInputField(value = emailAddress.value),
    )
}
