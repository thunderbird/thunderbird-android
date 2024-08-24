package app.k9mail.feature.account.setup.ui.options.display

import android.preference.Preference
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.State
import app.k9mail.feature.account.setup

internal fun AccountState.toDisplayOptionsState(): State {
    val options = displayOptions
    return if (options == null) {
        State(
            accountName = StringInputField(emailAddress ?: ""),
            // TODO: get display name from: preferences.defaultAccount?.senderName ?: ""
            displayName = StringInputField(""),
            emailSignature = StringInputField(""),
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
