package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import net.thunderbird.core.common.mail.Protocols

internal fun AccountState.toSyncOptionsState(): State {
    val options = syncOptions
    val defaultMessageDisplayCount = if (incomingServerSettings?.type in API_PROTOCOLS) {
        EmailDisplayCount.MESSAGES_10
    } else {
        EmailDisplayCount.DEFAULT
    }
    return if (options == null) {
        State(messageDisplayCount = defaultMessageDisplayCount)
    } else {
        State(
            checkFrequency = EmailCheckFrequency.fromMinutes(options.checkFrequencyInMinutes),
            messageDisplayCount = EmailDisplayCount.fromCount(options.messageDisplayCount),
            showNotification = options.showNotification,
        )
    }
}

private val API_PROTOCOLS = setOf(Protocols.GRAPH, Protocols.GMAIL_API, Protocols.ZOHO_MAIL_API)

internal fun State.toAccountSyncOptions(): AccountSyncOptions {
    return AccountSyncOptions(
        checkFrequencyInMinutes = checkFrequency.minutes,
        messageDisplayCount = messageDisplayCount.count,
        showNotification = showNotification,
    )
}
