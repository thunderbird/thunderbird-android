package app.k9mail.feature.account.edit.domain

sealed interface ApiAccountReauthorizationResult {
    data object Success : ApiAccountReauthorizationResult
    data object Canceled : ApiAccountReauthorizationResult
    data object NotAvailable : ApiAccountReauthorizationResult
    data object GmailAccountMismatch : ApiAccountReauthorizationResult
    data object Failed : ApiAccountReauthorizationResult
}
