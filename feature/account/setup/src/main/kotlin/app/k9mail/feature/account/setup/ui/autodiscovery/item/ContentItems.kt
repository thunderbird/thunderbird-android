package app.k9mail.feature.account.setup.ui.autodiscovery.item

import androidx.compose.foundation.lazy.LazyListScope
import app.k9mail.feature.account.common.ui.item.ListItem
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State

internal fun LazyListScope.contentItems(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
) {
    if (state.configStep != ConfigStep.EMAIL_ADDRESS) {
        item(key = "autodiscovery") {
            AutoDiscoveryResultItem(
                autoDiscoverySettings = state.autoDiscoverySettings,
                onEditConfigurationClick = { onEvent(Event.OnEditConfigurationClicked) },
            )
        }
        if (state.autoDiscoverySettings != null && state.autoDiscoverySettings.isTrusted.not()) {
            item(key = "result_approval") {
                AutoDiscoveryResultApprovalItem(
                    approvalState = state.configurationApproved,
                    onApprovalChange = { onEvent(Event.ResultApprovalChanged(it)) },
                )
            }
        }
    }

    item(key = "email") {
        EmailAddressItem(
            emailAddress = state.emailAddress.value,
            error = state.emailAddress.error,
            onEmailAddressChange = { onEvent(Event.EmailAddressChanged(it)) },
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
    } else if (state.configStep == ConfigStep.OAUTH) {
        item(key = "oauth") {
            ListItem {
                AccountOAuthView(
                    onOAuthResult = { result -> onEvent(Event.OnOAuthResult(result)) },
                    viewModel = oAuthViewModel,
                )
            }
        }
    }
}
