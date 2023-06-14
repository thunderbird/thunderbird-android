package app.k9mail.feature.account.setup.ui.autoconfig.item

import androidx.compose.foundation.lazy.LazyListScope
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.State

internal fun LazyListScope.contentItems(
    state: State,
    onEvent: (Event) -> Unit,
) {
    if (state.configStep == ConfigStep.PASSWORD) {
        item(key = "autodiscovery") {
            AutoDiscoveryStatusItem(
                autoDiscoverySettings = state.autoDiscoverySettings,
            )
        }
    }

    item(key = "email") {
        EmailAddressItem(
            emailAddress = state.emailAddress.value,
            error = state.emailAddress.error,
            onEmailAddressChange = { onEvent(Event.EmailAddressChanged(it)) },
            isEnabled = state.configStep == ConfigStep.EMAIL_ADDRESS,
        )
    }

    if (state.configStep == ConfigStep.PASSWORD) {
        item(key = "password") {
            PasswordItem(
                password = state.password.value,
                error = state.password.error,
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
            )
        }
    }
}
