package app.k9mail.feature.account.setup.ui.options

import app.k9mail.feature.account.setup.domain.entity.AccountOptions

internal fun AccountOptionsContract.State.toAccountOptions(): AccountOptions {
    return AccountOptions(
        accountName = accountName.value,
        displayName = displayName.value,
        emailSignature = emailSignature.value,
        checkFrequencyInMinutes = checkFrequency.minutes,
        messageDisplayCount = messageDisplayCount.count,
        showNotification = showNotification,
    )
}
