package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State

internal fun AccountState.toDisplayOptionsState(): State {
    val options = displayOptions
    return if (options == null) {
        State(
            accountName = StringInputField(emailAddress ?: ""),
            // displayName = StringInputField(""),
            // TODO: get display name from: preferences.defaultAccount?.senderName ?: ""
        )
    } else {
        State(
            accountName = StringInputField(options.accountName),
            displayName = StringInputField(options.displayName),
            emailSignature = StringInputField(options.emailSignature ?: ""),
        )
    }
}

internal fun State.toAccountDisplayOptions(): AccountDisplayOptions {
    return AccountDisplayOptions(
        accountName = accountName.value,
        displayName = displayName.value,
        emailSignature = emailSignature.value.takeIf { it.isNotEmpty() },
    )
}
