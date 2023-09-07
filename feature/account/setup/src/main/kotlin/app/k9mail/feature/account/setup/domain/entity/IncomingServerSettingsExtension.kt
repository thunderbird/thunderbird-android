package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType

internal fun IncomingServerSettings.toIncomingProtocolType(): IncomingProtocolType {
    when (this) {
        is ImapServerSettings -> return IncomingProtocolType.IMAP
        else -> throw IllegalArgumentException("Unsupported incoming server settings type: $this")
    }
}
