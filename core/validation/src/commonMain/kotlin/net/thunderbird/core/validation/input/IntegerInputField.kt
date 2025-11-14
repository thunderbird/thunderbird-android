package net.thunderbird.core.validation.input

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome

class IntegerInputField(
    override val value: Int? = null,
    override val error: ValidationError? = null,
    override val isValid: Boolean = false,
) : InputField<Int?> {

    override fun updateValue(value: Int?): IntegerInputField {
        return IntegerInputField(
            value = value,
            error = null,
            isValid = false,
        )
    }

    override fun updateError(error: ValidationError?): IntegerInputField {
        return IntegerInputField(
            value = value,
            error = error,
            isValid = false,
        )
    }

    override fun updateValidity(isValid: Boolean): IntegerInputField {
        if (isValid == this.isValid) return this

        return IntegerInputField(
            value = value,
            error = null,
            isValid = isValid,
        )
    }

    override fun updateFromValidationOutcome(outcome: ValidationOutcome): IntegerInputField {
        return when (outcome) {
            is Outcome.Success -> IntegerInputField(
                value = value,
                error = null,
                isValid = true,
            )

            is Outcome.Failure -> IntegerInputField(
                value = value,
                error = outcome.error,
                isValid = false,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntegerInputField

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
        return "IntegerInputField(value=$value, error=$error, isValid=$isValid)"
    }
}
