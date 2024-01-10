package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State

internal fun AccountState.toSyncOptionsState(): State {
    val options = syncOptions
    return if (options == null) {
        State()
    } else {
        State(
            checkFrequency = EmailCheckFrequency.fromMinutes(options.checkFrequencyInMinutes),
            messageDisplayCount = EmailDisplayCount.fromCount(options.messageDisplayCount),
            showNotification = options.showNotification,
        )
    }
}

internal fun State.toAccountSyncOptions(): AccountSyncOptions {
    return AccountSyncOptions(
        checkFrequencyInMinutes = checkFrequency.minutes,
        messageDisplayCount = messageDisplayCount.count,
        showNotification = showNotification,
    )
}
