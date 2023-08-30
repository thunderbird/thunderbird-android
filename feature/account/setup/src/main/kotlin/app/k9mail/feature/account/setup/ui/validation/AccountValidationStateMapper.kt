package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.feature.account.common.domain.entity.AccountState

internal fun AccountState.toValidationState(isIncomingValidation: Boolean): AccountValidationContract.State {
    return AccountValidationContract.State(
        emailAddress = emailAddress,
        serverSettings = if (isIncomingValidation) incomingServerSettings else outgoingServerSettings,
        isLoading = false,
        isSuccess = false,
        error = null,
    )
}
