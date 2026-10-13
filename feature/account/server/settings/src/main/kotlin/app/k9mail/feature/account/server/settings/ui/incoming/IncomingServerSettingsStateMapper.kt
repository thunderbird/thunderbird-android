package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.entity.toAuthType
import app.k9mail.feature.account.common.domain.entity.toAuthenticationType
import app.k9mail.feature.account.common.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.common.domain.entity.toMailConnectionSecurity
import app.k9mail.feature.account.server.settings.ui.common.toInvalidEmailDomain
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.State
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings
import com.fsck.k9.mail.store.imap.ImapStoreSettings.autoDetectNamespace
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isSendClientInfo
import com.fsck.k9.mail.store.imap.ImapStoreSettings.isUseCompression
import com.fsck.k9.mail.store.imap.ImapStoreSettings.pathPrefix
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.validation.input.NumberInputField
import net.thunderbird.core.validation.input.StringInputField

fun AccountState.toIncomingServerSettingsState(): State {
    val state = incomingServerSettings?.toIncomingServerSettingsStateOrNull()
        ?: State(
            username = StringInputField(value = emailAddress ?: ""),
            server = StringInputField(value = emailAddress?.toInvalidEmailDomain() ?: ""),
        )
    return state
}

// Server settings with an unsupported protocol (e.g. "demo" from the debug/daily DemoAutoDiscovery) are treated
// like missing settings, so the user ends up with the manual configuration defaults instead of a crash.
private fun ServerSettings.toIncomingServerSettingsStateOrNull(): State? {
    val protocolType = IncomingProtocolType.fromNameOrNull(type) ?: return null
    return State(
        protocolType = protocolType,
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
        preserveLegacyGraphProtocol = type == Protocols.GRAPH_LEGACY,
        apiOAuthClientId = StringInputField(value = extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID].orEmpty()),
        apiOAuthRedirectUri = StringInputField(
            value = extra[NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI].orEmpty(),
        ),
        zohoClientId = StringInputField(value = extra[ZohoMailServerSettings.EXTRA_CLIENT_ID].orEmpty()),
        zohoRedirectUri = StringInputField(value = extra[ZohoMailServerSettings.EXTRA_REDIRECT_URI].orEmpty()),
        zohoIncomingOnly = extra[ZohoMailServerSettings.EXTRA_INCOMING_ONLY].toBoolean(),
    )
}

internal fun State.toServerSettings(): ServerSettings {
    val storedPassword = when {
        protocolType == IncomingProtocolType.ZOHO_MAIL_API -> password.value.trim().takeIf(String::isNotEmpty)
        authenticationType.isPasswordRequired -> password.value.trim()
        else -> null
    }

    return ServerSettings(
        type = if (protocolType == IncomingProtocolType.GRAPH && preserveLegacyGraphProtocol) {
            Protocols.GRAPH_LEGACY
        } else {
            protocolType.defaultName
        },
        host = server.value.trim(),
        port = port.value!!.toInt(),
        connectionSecurity = security.toMailConnectionSecurity(),
        authenticationType = authenticationType.toAuthType(),
        username = username.value.trim(),
        password = storedPassword,
        clientCertificateAlias = clientCertificateAlias,
        extra = createExtras(),
    )
}

private fun State.createExtras(): Map<String, String?> {
    return when (protocolType) {
        IncomingProtocolType.IMAP -> ImapStoreSettings.createExtra(
            autoDetectNamespace = imapAutodetectNamespaceEnabled,
            pathPrefix = if (imapAutodetectNamespaceEnabled) null else imapPrefix.value.trim(),
            useCompression = imapUseCompression,
            sendClientInfo = imapSendClientInfo,
        )

        IncomingProtocolType.ZOHO_MAIL_API -> mapOf(
            ZohoMailServerSettings.EXTRA_CLIENT_ID to zohoClientId.value.trim(),
            ZohoMailServerSettings.EXTRA_REDIRECT_URI to zohoRedirectUri.value.trim(),
            ZohoMailServerSettings.EXTRA_INCOMING_ONLY to zohoIncomingOnly.toString(),
        )

        IncomingProtocolType.GRAPH, IncomingProtocolType.GMAIL_API -> buildMap {
            apiOAuthClientId.value.trim().takeIf(String::isNotBlank)?.let { clientId ->
                put(NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID, clientId)
            }
            apiOAuthRedirectUri.value.trim().takeIf(String::isNotBlank)?.let { redirectUri ->
                put(NativeApiMailServerSettings.EXTRA_OAUTH_REDIRECT_URI, redirectUri)
            }
        }

        else -> emptyMap()
    }
}
