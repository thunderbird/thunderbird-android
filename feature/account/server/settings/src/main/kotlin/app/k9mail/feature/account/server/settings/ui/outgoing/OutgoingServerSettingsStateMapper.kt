package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.toAuthType
import app.k9mail.feature.account.common.domain.entity.toAuthenticationType
import app.k9mail.feature.account.common.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.State
import com.fsck.k9.mail.MailProxySettings
import com.fsck.k9.mail.MailProxyType
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

fun AccountState.toOutgoingServerSettingsState(): State {
    val password = getOutgoingServerPassword()

    return outgoingServerSettings?.toOutgoingServerSettingsState(password)
        ?: State(
            username = StringInputField(value = emailAddress ?: ""),
            password = StringInputField(value = password),
            server = StringInputField(value = emailAddress?.toInvalidEmailDomain() ?: ""),
            proxyType = defaultProxySettings.type,
            proxyServer = StringInputField(value = defaultProxySettings.host.orEmpty()),
            proxyPort = NumberInputField(value = defaultProxySettings.port.takeIf { it > 0 }?.toLong()),
            proxyDns = defaultProxySettings.proxyDns,
            proxyUsername = StringInputField(value = defaultProxySettings.username.orEmpty()),
            proxyPassword = StringInputField(value = defaultProxySettings.password.orEmpty()),
        )
}

private fun AccountState.getOutgoingServerPassword(): String {
    return if (outgoingServerSettings?.authenticationType?.toAuthenticationType()?.isPasswordRequired == false) {
        ""
    } else {
        outgoingServerSettings?.password ?: incomingServerSettings?.password ?: ""
    }
}

private fun ServerSettings.toOutgoingServerSettingsState(password: String): State {
    val proxySettings = MailProxySettings.fromServerSettings(this)

    return State(
        server = StringInputField(value = host),
        security = connectionSecurity.toConnectionSecurity(),
        port = NumberInputField(value = port.toLong()),
        authenticationType = authenticationType.toAuthenticationType(),
        username = StringInputField(value = username),
        password = StringInputField(value = password),
        clientCertificateAlias = clientCertificateAlias,
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
        type = "smtp",
        host = server.value.trim(),
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = if (authenticationType.isUsernameRequired) username.value.trim() else "",
        password = if (authenticationType.isPasswordRequired) password.value.trim() else null,
        clientCertificateAlias = clientCertificateAlias,
        extra = createProxySettings().toExtra(),
    )
}

private fun State.createProxySettings(): MailProxySettings {
    return when (proxyType) {
        MailProxyType.USE_GLOBAL -> MailProxySettings.USE_GLOBAL

        MailProxyType.NONE -> MailProxySettings.NONE

        else -> MailProxySettings(
            type = proxyType,
            host = proxyServer.value.trim(),
            port = proxyPort.value!!.toInt(),
            proxyDns = proxyDns,
            username = proxyUsername.value.trim().ifBlank { null },
            password = proxyPassword.value.ifBlank { null },
        )
    }
}
