package net.thunderbird.core.validation.input

import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome

/**
 * InputField is an interface defining the state of an input field.
 *
 * @param T The type of the value the input field holds.
 */
interface InputField<T> {
    val value: T
    val error: ValidationError?
    val isValid: Boolean

    /**
     * Updates the current value of the input field.
     *
     * @param value The new value to be set for the input field.
     * @return a new InputField instance with the updated value.
     */
    fun updateValue(value: T): InputField<T>

    /**
     * Updates the current error of the input field.
     *
     * @param error The new error to be set for the input field.
     */
    fun updateError(error: ValidationError?): InputField<T>

    /**
     * Updates the current validity of the input field.
     *
     * @param isValid The new validity to be set for the input field.
     */
    fun updateValidity(isValid: Boolean): InputField<T>

    /**
     * Checks if the input field currently has an error.
     *
     * @return a Boolean indicating whether the input field has an error.
     */
    fun hasError(): Boolean {
        return error != null
    }

    fun updateFromValidationOutcome(outcome: ValidationOutcome): InputField<T>
}
