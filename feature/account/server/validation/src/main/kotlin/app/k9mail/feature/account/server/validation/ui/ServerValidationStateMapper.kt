package app.k9mail.feature.account.server.validation.ui

import app.k9mail.feature.account.common.domain.entity.AccountState

internal fun AccountState.toServerValidationState(isIncomingValidation: Boolean): ServerValidationContract.State {
    return ServerValidationContract.State(
        emailAddress = emailAddress,
        serverSettings = if (isIncomingValidation) incomingServerSettings else outgoingServerSettings,
        isLoading = false,
        isSuccess = false,
        error = null,
    )
}
