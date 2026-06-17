package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.toAuthType
import app.k9mail.feature.account.common.domain.entity.toAuthenticationType
import app.k9mail.feature.account.common.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.MailProxyType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

fun AccountState.toIncomingServerSettingsState() = incomingServerSettings?.toIncomingServerSettingsState()
    ?: State(
        username = StringInputField(value = emailAddress ?: ""),
        server = StringInputField(value = emailAddress?.toInvalidEmailDomain() ?: ""),
    )

private fun ServerSettings.toIncomingServerSettingsState(): State {
    val proxySettings = MailProxySettings.fromServerSettings(this)

    return State(
        protocolType = IncomingProtocolType.fromName(type),
        server = StringInputField(value = host),
        security = connectionSecurity.toConnectionSecurity(),
        port = NumberInputField(value = port.toLong()),
        authenticationType = authenticationType.toAuthenticationType(),
        username = StringInputField(value = username),
        password = StringInputField(value = password ?: ""),
        clientCertificateAlias = clientCertificateAlias,
        imapAutodetectNamespaceEnabled = autoDetectNamespace,
        imapPrefix = StringInputField(value = pathPrefix ?: ""),
        imapUseCompression = isUseCompression,
        imapSendClientInfo = isSendClientInfo,
        proxyType = proxySettings.type,
        proxyServer = StringInputField(value = proxySettings.host ?: ""),
        proxyPort = NumberInputField(value = proxySettings.port.takeIf { it > 0 }?.toLong()),
        proxyDns = proxySettings.proxyDns,
        proxyUsername = StringInputField(value = proxySettings.username ?: ""),
        proxyPassword = StringInputField(value = proxySettings.password ?: ""),
    )
}

internal fun State.toServerSettings(): ServerSettings {
    return ServerSettings(
        type = protocolType.defaultName,
        host = server.value.trim(),
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = username.value.trim(),
        password = if (authenticationType.isPasswordRequired) password.value.trim() else null,
        clientCertificateAlias = clientCertificateAlias,
        extra = createExtras(),
    )
}

private fun State.createExtras(): Map<String, String?> {
    val protocolExtras = if (protocolType == IncomingProtocolType.IMAP) {
        ImapStoreSettings.createExtra(
            autoDetectNamespace = imapAutodetectNamespaceEnabled,
            pathPrefix = if (imapAutodetectNamespaceEnabled) null else imapPrefix.value.trim(),
            useCompression = imapUseCompression,
            sendClientInfo = imapSendClientInfo,
        )
    } else {
        emptyMap()
    }

    return protocolExtras + createProxySettings().toExtra()
}

private fun State.createProxySettings(): MailProxySettings {
    return if (proxyType == MailProxyType.NONE) {
        MailProxySettings.NONE
    } else {
        MailProxySettings(
            type = proxyType,
            host = proxyServer.value.trim(),
            port = proxyPort.value!!.toInt(),
            proxyDns = proxyDns,
            username = proxyUsername.value.trim().ifBlank { null },
            password = proxyPassword.value.ifBlank { null },
        )
    }
}
