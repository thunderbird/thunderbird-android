package app.k9mail.feature.account.edit.domain

sealed interface GraphAccountReauthorizationResult {
    data object Success : GraphAccountReauthorizationResult
    data object Canceled : GraphAccountReauthorizationResult
    data object NotAvailable : GraphAccountReauthorizationResult
    data object MicrosoftAccountMismatch : GraphAccountReauthorizationResult
    data object Failed : GraphAccountReauthorizationResult
}
