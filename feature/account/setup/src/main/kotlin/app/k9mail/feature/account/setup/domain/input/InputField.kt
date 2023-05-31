package app.k9mail.feature.account.setup.domain.input

/**
 * InputField is an interface defining the state of an input field.
 *
 * @param T The type of the value the input field holds.
 */
interface InputField<T> {
    val value: T
    val errorMessage: String?
    val isValid: Boolean

    /**
     * Updates the current value of the input field.
     *
     * @param value The new value to be set for the input field.
     * @return a new InputField instance with the updated value.
     */
    fun updateValue(value: T): InputField<T>

    /**
     * Updates the current error message of the input field.
     *
     * @param errorMessage The new error message to be set for the input field.
     */
    fun updateErrorMessage(errorMessage: String?): InputField<T>

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
        return errorMessage != null
    }
}
