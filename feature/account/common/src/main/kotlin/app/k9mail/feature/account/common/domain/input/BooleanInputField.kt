package app.k9mail.feature.account.common.domain.input

import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

class BooleanInputField(
    override val value: Boolean? = null,
    override val error: ValidationError? = null,
    override val isValid: Boolean = false,
) : InputField<Boolean?> {
    override fun updateValue(value: Boolean?): BooleanInputField {
        return BooleanInputField(
            value = value,
            error = null,
            isValid = false,
        )
    }

    override fun updateError(error: ValidationError?): BooleanInputField {
        return BooleanInputField(
            value = value,
            error = error,
            isValid = false,
        )
    }

    override fun updateValidity(isValid: Boolean): BooleanInputField {
        if (isValid == this.isValid) return this

        return BooleanInputField(
            value = value,
            error = null,
            isValid = isValid,
        )
    }

    override fun updateFromValidationResult(result: ValidationResult): BooleanInputField {
        return when (result) {
            is ValidationResult.Success -> BooleanInputField(
                value = value,
                error = null,
                isValid = true,
            )

            is ValidationResult.Failure -> BooleanInputField(
                value = value,
                error = result.error,
                isValid = false,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BooleanInputField

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
        return "BooleanInputField(value=$value, error=$error, isValid=$isValid)"
    }
}
