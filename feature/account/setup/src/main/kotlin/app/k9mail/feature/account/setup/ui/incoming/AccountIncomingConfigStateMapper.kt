package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toAuthType
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.State
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings

internal fun AccountState.toIncomingConfigState(): State {
    val incomingServerSettings = incomingServerSettings
    return if (incomingServerSettings == null) {
        State(
            username = StringInputField(value = emailAddress ?: ""),
        )
    } else {
        State(
            protocolType = IncomingProtocolType.fromName(incomingServerSettings.type),
            server = StringInputField(value = incomingServerSettings.host ?: ""),
            security = incomingServerSettings.connectionSecurity.toConnectionSecurity(),
            port = NumberInputField(value = incomingServerSettings.port.toLong()),
            authenticationType = incomingServerSettings.authenticationType.toAuthenticationType(),
            username = StringInputField(value = incomingServerSettings.username),
            password = StringInputField(value = incomingServerSettings.password ?: ""),
        )
    }
}

internal fun State.toServerSettings(): ServerSettings {
    return ServerSettings(
        type = protocolType.defaultName,
        host = server.value,
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = username.value,
        password = if (authenticationType.isPasswordRequired) password.value else null,
        clientCertificateAlias = clientCertificateAlias,
        extra = createExtras(),
    )
}

private fun State.createExtras(): Map<String, String?> {
    return if (protocolType == IncomingProtocolType.IMAP) {
        ImapStoreSettings.createExtra(
            autoDetectNamespace = imapAutodetectNamespaceEnabled,
            pathPrefix = if (imapAutodetectNamespaceEnabled) null else imapPrefix.value,
            useCompression = imapUseCompression,
            sendClientId = imapSendClientId,
        )
    } else {
        emptyMap()
    }
}

internal fun State.toValidationState(): AccountValidationContract.State {
    return AccountValidationContract.State(
        serverSettings = toServerSettings(),
        // TODO add authorization state
    )
}
