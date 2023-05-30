package app.k9mail.core.common.domain.usecase.validation

interface ValidationResult {
    object Success : ValidationResult

    data class Failure(val error: Exception) : ValidationResult
}
