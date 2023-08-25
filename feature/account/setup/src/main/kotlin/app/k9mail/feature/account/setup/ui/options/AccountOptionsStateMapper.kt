package app.k9mail.feature.account.setup.ui.options

import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State

internal fun AccountSetupState.toAccountOptionsState(): State {
    val options = options
    return if (options == null) {
        State(
            accountName = StringInputField(emailAddress ?: ""),
        )
    } else {
        State(
            accountName = StringInputField(options.accountName),
            displayName = StringInputField(options.displayName),
            emailSignature = StringInputField(options.emailSignature ?: ""),
            checkFrequency = EmailCheckFrequency.fromMinutes(options.checkFrequencyInMinutes),
            messageDisplayCount = EmailDisplayCount.fromCount(options.messageDisplayCount),
            showNotification = options.showNotification,
        )
    }
}

internal fun State.toAccountOptions(): AccountOptions {
    return AccountOptions(
        accountName = accountName.value,
        displayName = displayName.value,
        emailSignature = emailSignature.value.takeIf { it.isNotEmpty() },
        checkFrequencyInMinutes = checkFrequency.minutes,
        messageDisplayCount = messageDisplayCount.count,
        showNotification = showNotification,
    )
}
