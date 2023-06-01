package app.k9mail.feature.account.setup.domain.input

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult

data class StringInputField(
    override val value: String = "",
    override val error: ValidationError? = null,
    override val isValid: Boolean = false,
) : InputField<String> {

    override fun updateValue(value: String): StringInputField {
        return StringInputField(
            value = value,
            error = null,
            isValid = false,
        )
    }

    override fun updateError(error: ValidationError?): StringInputField {
        return StringInputField(
            value = value,
            error = error,
            isValid = false,
        )
    }

    override fun updateValidity(isValid: Boolean): StringInputField {
        if (isValid == this.isValid) return this

        return StringInputField(
            value = value,
            error = null,
            isValid = isValid,
        )
    }
}

fun StringInputField.fromValidationResult(result: ValidationResult): StringInputField {
    return when (result) {
        is ValidationResult.Success -> copy(
            error = null,
            isValid = true,
        )

        is ValidationResult.Failure -> copy(
            error = result.error,
            isValid = false,
        )
    }
}
