package net.thunderbird.core.common.domain.usecase.validation

sealed interface ValidationResult {
    data object Success : ValidationResult

    data class Failure(val error: ValidationError) : ValidationResult
}
