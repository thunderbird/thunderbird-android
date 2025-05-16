package app.k9mail.feature.account.common.domain.input

import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

class NumberInputField(
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
            is ValidationResult.Success -> NumberInputField(
                value = value,
                error = null,
                isValid = true,
            )

            is ValidationResult.Failure -> NumberInputField(
                value = value,
                error = result.error,
                isValid = false,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NumberInputField

        if (value != other.value) return false
        if (error != other.error) return false
        return isValid == other.isValid
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isValid.hashCode()
        return result
    }

    override fun toString(): String {
        return "NumberInputField(value=$value, error=$error, isValid=$isValid)"
    }
}
