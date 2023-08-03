package app.k9mail.feature.account.setup.ui.validation

import app.k9mail.feature.account.setup.domain.entity.AccountSetupState

internal fun AccountSetupState.toValidationState(isIncomingValidation: Boolean): AccountValidationContract.State {
    return AccountValidationContract.State(
        serverSettings = if (isIncomingValidation) incomingServerSettings else outgoingServerSettings,
        authorizationState = authorizationState,
        isLoading = false,
        isSuccess = false,
        error = null,
    )
}
