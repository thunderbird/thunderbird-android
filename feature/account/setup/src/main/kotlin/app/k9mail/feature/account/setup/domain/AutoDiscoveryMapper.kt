package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.api.OutgoingServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.autodiscovery.demo.DemoServerSettings
import app.k9mail.feature.account.common.domain.entity.toAuthType
import app.k9mail.feature.account.common.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toAuthenticationType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings

internal fun IncomingServerSettings.toServerSettings(password: String?): ServerSettings {
    return when (this) {
        is ImapServerSettings -> this.toImapServerSettings(password)
        is DemoServerSettings -> this.serverSettings

        else -> throw IllegalArgumentException("Unknown server settings type: $this")
    }
}

private fun ImapServerSettings.toImapServerSettings(password: String?): ServerSettings {
    return ServerSettings(
        type = "imap",
        host = hostname.value,
        port = port.value,
        connectionSecurity = connectionSecurity.toConnectionSecurity().toMailConnectionSecurity(),
        authenticationType = authenticationTypes.first().toAuthenticationType().toAuthType(),
        username = username,
        password = password,
        clientCertificateAlias = null,
        extra = ImapStoreSettings.createExtra(
            autoDetectNamespace = true,
            pathPrefix = null,
            useCompression = true,
            sendClientInfo = true,
        ),
    )
}

/**
 * Convert [OutgoingServerSettings] to [ServerSettings].
 *
 * @throws IllegalArgumentException if the server settings type is unknown.
 */
internal fun OutgoingServerSettings.toServerSettings(password: String?): ServerSettings {
    return when (this) {
        is SmtpServerSettings -> this.toSmtpServerSettings(password)
        is DemoServerSettings -> this.serverSettings

        else -> throw IllegalArgumentException("Unknown server settings type: $this")
    }
}

private fun SmtpServerSettings.toSmtpServerSettings(password: String?): ServerSettings {
    return ServerSettings(
        type = "smtp",
        host = hostname.value,
        port = port.value,
        connectionSecurity = connectionSecurity.toConnectionSecurity().toMailConnectionSecurity(),
        authenticationType = authenticationTypes.first().toAuthenticationType().toAuthType(),
        username = username,
        password = password,
        clientCertificateAlias = null,
        extra = emptyMap(),
    )
}
