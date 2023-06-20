package app.k9mail.feature.account.setup.ui.common.mapper

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.SmtpServerSettings
import app.k9mail.feature.account.setup.domain.entity.ConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toConnectionSecurity
import app.k9mail.feature.account.setup.domain.entity.toImapDefaultPort
import app.k9mail.feature.account.setup.domain.entity.toIncomingProtocolType
import app.k9mail.feature.account.setup.domain.entity.toSmtpDefaultPort
import app.k9mail.feature.account.setup.domain.input.NumberInputField
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract

internal fun AccountAutoDiscoveryContract.State.toIncomingConfigState(): AccountIncomingConfigContract.State {
    val incomingSettings = autoDiscoverySettings?.incomingServerSettings as? ImapServerSettings?
    return AccountIncomingConfigContract.State(
        protocolType = incomingSettings?.toIncomingProtocolType() ?: IncomingProtocolType.DEFAULT,
        server = StringInputField(
            value = prefillServer(
                hostname = incomingSettings?.hostname?.value,
            ),
        ),
        security = incomingSettings?.connectionSecurity?.toConnectionSecurity() ?: ConnectionSecurity.DEFAULT,
        port = NumberInputField(
            value = incomingSettings?.port?.value?.toLong() ?: ConnectionSecurity.DEFAULT.toImapDefaultPort(),
        ),
        username = StringInputField(
            value = prefillUserName(
                emailAddress = emailAddress.value,
                username = incomingSettings?.username,
            ),
        ),
        password = StringInputField(value = password.value),
    )
}

internal fun AccountAutoDiscoveryContract.State.toOutgoingConfigState(): AccountOutgoingConfigContract.State {
    val outgoingSettings = autoDiscoverySettings?.outgoingServerSettings as? SmtpServerSettings?
    return AccountOutgoingConfigContract.State(
        server = StringInputField(
            value = prefillServer(
                hostname = outgoingSettings?.hostname?.value,
            ),
        ),
        security = outgoingSettings?.connectionSecurity?.toConnectionSecurity() ?: ConnectionSecurity.DEFAULT,
        port = NumberInputField(
            value = outgoingSettings?.port?.value?.toLong() ?: ConnectionSecurity.DEFAULT.toSmtpDefaultPort(),
        ),
        username = StringInputField(
            value = prefillUserName(
                emailAddress = emailAddress.value,
                username = outgoingSettings?.username,
            ),
        ),
        password = StringInputField(value = password.value),
    )
}

private fun prefillServer(hostname: String?) = hostname.orEmpty()

internal fun prefillUserName(
    emailAddress: String,
    username: String?,
): String {
    return username.takeUnless { it.isNullOrEmpty() } ?: emailAddress
}

internal fun AccountAutoDiscoveryContract.State.toOptionsState(): AccountOptionsContract.State {
    return AccountOptionsContract.State(
        accountName = StringInputField(value = emailAddress.value),
    )
}
