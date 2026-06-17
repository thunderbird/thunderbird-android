package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toIncomingProtocolType
import app.k9mail.feature.account.setup.domain.toServerSettings
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract
import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

internal fun AccountAutoDiscoveryContract.State.toAccountState(): AccountState {
    val proxySettings = toProxySettings()

    return AccountState(
        emailAddress = emailAddress.value,
        incomingServerSettings = autoDiscoverySettings?.incomingServerSettings
            ?.toServerSettings(password.value)
            ?.withProxySettings(proxySettings),
        outgoingServerSettings = autoDiscoverySettings?.outgoingServerSettings
            ?.toServerSettings(password.value)
            ?.withProxySettings(proxySettings),
        defaultProxySettings = proxySettings,
        authorizationState = authorizationState,
        displayOptions = null,
        syncOptions = null,
    )
}

internal fun AccountAutoDiscoveryContract.State.toIncomingConfigState(): IncomingServerSettingsContract.State {
    val incomingSettings = autoDiscoverySettings?.incomingServerSettings as? ImapServerSettings?
    val proxySettings = toProxySettings()
    return if (incomingSettings == null) {
        IncomingServerSettingsContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
            proxyType = proxySettings.type,
            proxyServer = StringInputField(value = proxySettings.host.orEmpty()),
            proxyPort = NumberInputField(value = proxySettings.port.takeIf { it > 0 }?.toLong()),
            proxyDns = proxySettings.proxyDns,
            proxyUsername = StringInputField(value = proxySettings.username.orEmpty()),
            proxyPassword = StringInputField(value = proxySettings.password.orEmpty()),
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
    val proxySettings = toProxySettings()
    return if (outgoingSettings == null) {
        OutgoingServerSettingsContract.State(
            username = StringInputField(value = emailAddress.value),
            password = StringInputField(value = password.value),
            proxyType = proxySettings.type,
            proxyServer = StringInputField(value = proxySettings.host.orEmpty()),
            proxyPort = NumberInputField(value = proxySettings.port.takeIf { it > 0 }?.toLong()),
            proxyDns = proxySettings.proxyDns,
            proxyUsername = StringInputField(value = proxySettings.username.orEmpty()),
            proxyPassword = StringInputField(value = proxySettings.password.orEmpty()),
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

private fun ServerSettings.withProxySettings(proxySettings: MailProxySettings): ServerSettings {
    return copy(extra = extra + proxySettings.toExtra())
}

internal fun AccountAutoDiscoveryContract.State.toOptionsState(): DisplayOptionsContract.State {
    return DisplayOptionsContract.State(
        accountName = StringInputField(value = emailAddress.value),
    )
}
