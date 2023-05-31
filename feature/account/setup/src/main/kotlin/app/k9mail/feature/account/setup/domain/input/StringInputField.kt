package app.k9mail.feature.account.setup.domain.input

data class StringInputField(
    override val value: String = "",
    override val errorMessage: String? = null,
    override val isValid: Boolean = false,
) : InputField<String> {

    override fun updateValue(value: String): StringInputField {
        return StringInputField(
            value = value,
            errorMessage = null,
            isValid = false,
        )
    }

    override fun updateErrorMessage(errorMessage: String?): StringInputField {
        return StringInputField(
            value = value,
            errorMessage = errorMessage,
            isValid = false,
        )
    }

    override fun updateValidity(isValid: Boolean): StringInputField {
        if (isValid == this.isValid) return this

        return StringInputField(
            value = value,
            errorMessage = null,
            isValid = isValid,
        )
    }
}
