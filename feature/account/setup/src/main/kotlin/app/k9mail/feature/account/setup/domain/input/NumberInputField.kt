package app.k9mail.feature.account.setup.domain.input

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult

data class NumberInputField(
    override val value: Long? = null,
    override val error: ValidationError? = null,
    override val isValid: Boolean = false,
) : InputField<Long?> {

    override fun updateValue(value: Long?): NumberInputField {
        return NumberInputField(
            value = value,
            error = null,
            isValid = false,
        )
    }

    override fun updateError(error: ValidationError?): NumberInputField {
        return NumberInputField(
            value = value,
            error = error,
            isValid = false,
        )
    }

    override fun updateValidity(isValid: Boolean): NumberInputField {
        if (isValid == this.isValid) return this

        return NumberInputField(
            value = value,
            error = null,
            isValid = isValid,
        )
    }

    override fun updateFromValidationResult(result: ValidationResult): NumberInputField {
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
}
